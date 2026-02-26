package com.groupf.dating.service.impl;

import com.groupf.dating.common.ToneType;
import com.groupf.dating.dto.ConversationStarterRequest;
import com.groupf.dating.dto.ConversationStarterResponse;
import com.groupf.dating.model.ProfileOptimizationRequest;
import com.groupf.dating.repository.ProfileRequestRepository;
import com.groupf.dating.service.ClaudeApiService;
import com.groupf.dating.service.ConversationStarterService;
import com.groupf.dating.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationStarterServiceImpl implements ConversationStarterService {

    private final ClaudeApiService claudeApiService;
    private final ProfileRequestRepository profileRequestRepository;

    @Override
    public ConversationStarterResponse generateStarters(ConversationStarterRequest request) {
        ToneType tone = ToneType.fromString(request.getTone());

        String systemPrompt = PromptBuilder.buildConversationStarterSystemPrompt(tone);
        String userPrompt = PromptBuilder.buildConversationStarterUserPrompt(request.getBio());

        log.info("Generating conversation starters with tone: {}", tone.getValue());

        String response = claudeApiService.callClaudeApi(systemPrompt, userPrompt);
        ConversationStarterResponse result = parseConversationStarters(response, request.getBio(), tone.getValue());
        log.info("Successfully generated {} conversation starters", result.getStarters().size());

        saveToDatabase(request.getBio(), tone.getValue(), result.getStarters());

        return result;
    }

    private void saveToDatabase(String bio, String tone, java.util.List<String> starters) {
        try {
            ProfileOptimizationRequest entity = new ProfileOptimizationRequest();
            entity.setOriginalBio(bio);
            entity.setTonePreference(tone);
            entity.setConversationStarters(starters);
            entity.setCreatedAt(java.time.LocalDateTime.now());
            profileRequestRepository.save(entity);
            log.info("Conversation starter result saved to database");
        } catch (Exception e) {
            log.error("Failed to save conversation starter result to database", e);
        }
    }

    /**
     * Parses Claude's response to extract conversation starters
     */
    private ConversationStarterResponse parseConversationStarters(String response, String bio, String tone) {
        List<String> starters = new ArrayList<>();

        // Try to extract numbered starters (1. 2. 3. or 1) 2) 3))
        Pattern pattern = Pattern.compile("(?:^|\\n)\\s*\\d+[.):]\\s*(.+?)(?=\\n\\s*\\d+[.):]|$)",
                Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            String starter = matcher.group(1).trim();
            // Clean up the starter (remove quotes, extra whitespace)
            starter = starter.replaceAll("^[\"']|[\"']$", "").trim();

            if (starter.length() > 10) {
                starters.add(starter);
            }
        }

        // If parsing failed, try splitting by newlines
        if (starters.isEmpty()) {
            String[] lines = response.split("\\n");
            for (String line : lines) {
                line = line.trim();
                line = line.replaceAll("^[-*•]\\s*", ""); // Remove bullet points
                line = line.replaceAll("^[\"']|[\"']$", ""); // Remove quotes

                // Skip empty lines and headers
                if (line.length() > 10
                        && !line.toLowerCase().contains("conversation starter")
                        && !line.toLowerCase().contains("opening message")
                        && !line.toLowerCase().contains("here are")
                        && !line.endsWith(":")) {
                    starters.add(line);
                    if (starters.size() >= 5) break;
                }
            }
        }

        // Fallback: if still no results, split by periods or semicolons
        if (starters.isEmpty()) {
            String[] sentences = response.split("[.;]\\s+");
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.length() > 10 && sentence.length() < 200) {
                    starters.add(sentence);
                    if (starters.size() >= 3) break;
                }
            }
        }

        // Last resort: return the entire response
        if (starters.isEmpty()) {
            starters.add(response.trim());
        }

        return new ConversationStarterResponse(bio, starters, tone);
    }
}
