package com.groupf.dating.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupf.dating.dto.PhotoRankResponse;
import com.groupf.dating.exception.IException;
import com.groupf.dating.exception.ErrorCode;
import com.groupf.dating.model.ProfileOptimizationRequest;
import com.groupf.dating.repository.ProfileRequestRepository;
import com.groupf.dating.service.ClaudeApiService;
import com.groupf.dating.service.PhotoRankingService;
import com.groupf.dating.util.ImageUtil;
import com.groupf.dating.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoRankingServiceImpl implements PhotoRankingService {

    private final ClaudeApiService claudeApiService;
    private final ObjectMapper objectMapper;
    private final ProfileRequestRepository profileRequestRepository;

    @Override
    public PhotoRankResponse rankPhotos(MultipartFile[] photos) {
        // Validate photo count
        if (photos == null || photos.length == 0) {
            throw new IException(ErrorCode.PHOTO_TOO_FEW);
        }

        if (photos.length < com.groupf.dating.common.AppConstants.PHOTO_MIN_COUNT) {
            throw new IException(ErrorCode.PHOTO_TOO_FEW);
        }
        if (photos.length > com.groupf.dating.common.AppConstants.PHOTO_MAX_COUNT) {
            throw new IException(ErrorCode.PHOTO_TOO_MANY);
        }

        // Validate and convert photos
        List<ClaudeApiService.ImageContent> imageContents = new ArrayList<>();
        List<String> photoNames = new ArrayList<>();

        for (int i = 0; i < photos.length; i++) {
            MultipartFile photo = photos[i];

            if (!ImageUtil.isValidImage(photo)) {
                throw new IException(ErrorCode.PHOTO_INVALID_FORMAT,
                    "Invalid image: " + photo.getOriginalFilename());
            }

            try {
                String base64 = ImageUtil.convertToBase64(photo);
                String mediaType = ImageUtil.getMediaType(photo);
                imageContents.add(new ClaudeApiService.ImageContent(base64, mediaType));
                photoNames.add(photo.getOriginalFilename() != null ?
                        photo.getOriginalFilename() : "Photo " + (i + 1));
            } catch (IOException e) {
                log.error("Failed to process image: {}", photo.getOriginalFilename(), e);
                throw new IException(ErrorCode.PHOTO_PROCESS_FAILED,
                    "Failed to process image: " + photo.getOriginalFilename());
            }
        }

        String systemPrompt = PromptBuilder.buildPhotoRankingSystemPrompt();
        String userPrompt = PromptBuilder.buildPhotoRankingUserPrompt(photos.length);

        log.info("Ranking {} photos", photos.length);

        String response = claudeApiService.callClaudeApiWithVision(systemPrompt, userPrompt, imageContents);
        PhotoRankResponse result = parsePhotoRankings(response, photoNames);
        log.info("Successfully ranked {} photos", result.getRankedPhotos().size());

        saveToDatabase(photoNames, result.getRankedPhotos());

        return result;
    }

    private void saveToDatabase(List<String> photoNames, List<PhotoRankResponse.RankedPhoto> rankedPhotos) {
        try {
            ProfileOptimizationRequest entity = new ProfileOptimizationRequest();
            entity.setPhotoUrls(photoNames);

            List<ProfileOptimizationRequest.PhotoRanking> rankings = rankedPhotos.stream()
                    .map(rp -> new ProfileOptimizationRequest.PhotoRanking(
                            rp.getPhotoName(),
                            rp.getRank(),
                            rp.getScore(),
                            rp.getReasoning()))
                    .toList();
            entity.setRankedPhotos(rankings);
            entity.setCreatedAt(java.time.LocalDateTime.now());
            profileRequestRepository.save(entity);
            log.info("Photo ranking result saved to database");
        } catch (Exception e) {
            log.error("Failed to save photo ranking result to database", e);
        }
    }

    /**
     * Parses Claude's response to extract photo rankings
     */
    private PhotoRankResponse parsePhotoRankings(String response, List<String> photoNames) {
        List<PhotoRankResponse.RankedPhoto> rankedPhotos = new ArrayList<>();

        try {
            // Try to parse as JSON first
            if (response.trim().startsWith("[")) {
                List<Map<String, Object>> rankings = objectMapper.readValue(
                        response, new TypeReference<List<Map<String, Object>>>() {});

                for (int i = 0; i < rankings.size() && i < photoNames.size(); i++) {
                    Map<String, Object> ranking = rankings.get(i);
                    rankedPhotos.add(new PhotoRankResponse.RankedPhoto(
                            photoNames.get(i),
                            getIntValue(ranking, "rank", i + 1),
                            getDoubleValue(ranking, "score", 0.0),
                            (String) ranking.getOrDefault("reasoning", "No reasoning provided"),
                            null // Don't include base64 in response to reduce size
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON response, falling back to text parsing", e);
        }

        // Fallback: parse text response
        if (rankedPhotos.isEmpty()) {
            rankedPhotos = parseTextRankings(response, photoNames);
        }

        // Sort by rank
        rankedPhotos.sort(Comparator.comparingInt(PhotoRankResponse.RankedPhoto::getRank));

        return new PhotoRankResponse(rankedPhotos);
    }

    /**
     * Parses text-based ranking response
     */
    private List<PhotoRankResponse.RankedPhoto> parseTextRankings(String response, List<String> photoNames) {
        List<PhotoRankResponse.RankedPhoto> rankedPhotos = new ArrayList<>();

        // Pattern to match rankings like "1. Rank: 1, Score: 85, Reasoning: ..."
        Pattern pattern = Pattern.compile(
                "(?:Photo\\s*\\d+|\\d+[.)])\\s*(?:Rank|#)?:?\\s*(\\d+).*?" +
                "(?:Score|Rating)\\s*:?\\s*(\\d+).*?" +
                        "(?:Reasoning|Reason|Why)\\s*:?\\s*(.+?)(?=\\n\\s*(?:Photo|\\d+[.)])|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = pattern.matcher(response);
        int index = 0;

        while (matcher.find() && index < photoNames.size()) {
            try {
                int rank = Integer.parseInt(matcher.group(1));
                double score = Double.parseDouble(matcher.group(2));
                String reasoning = matcher.group(3).trim();

                rankedPhotos.add(new PhotoRankResponse.RankedPhoto(
                        photoNames.get(index),
                        rank,
                        score,
                        reasoning,
                        null
                ));
                index++;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse ranking numbers", e);
            }
        }

        // If parsing failed, create default rankings
        if (rankedPhotos.isEmpty()) {
            for (int i = 0; i < photoNames.size(); i++) {
                rankedPhotos.add(new PhotoRankResponse.RankedPhoto(
                        photoNames.get(i),
                        i + 1,
                        70.0, // Default score
                        "Ranking analysis: " + response,
                        null
                ));
            }
        }

        return rankedPhotos;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}
