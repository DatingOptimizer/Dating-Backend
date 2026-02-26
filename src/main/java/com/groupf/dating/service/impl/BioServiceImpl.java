package com.groupf.dating.service.impl;

import com.groupf.dating.common.ToneType;
import com.groupf.dating.dto.BioRewriteRequest;
import com.groupf.dating.dto.BioRewriteResponse;
import com.groupf.dating.service.BioService;
import com.groupf.dating.service.ClaudeApiService;
import com.groupf.dating.util.PromptBuilder;
import com.groupf.dating.util.ValidationUtil;
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
public class BioServiceImpl implements BioService {

    private final ClaudeApiService claudeApiService;

    @Override
    public BioRewriteResponse rewriteBio(BioRewriteRequest request) {
        // Validate input
        String validationError = ValidationUtil.getBioValidationError(request.getBio());
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        ToneType tone = ToneType.fromString(request.getTone());

        String systemPrompt = PromptBuilder.buildBioRewriteSystemPrompt(tone);
        String userPrompt = PromptBuilder.buildBioRewriteUserPrompt(request.getBio());

        log.info("Rewriting bio with tone: {}", tone.getValue());

        String response = claudeApiService.callClaudeApi(systemPrompt, userPrompt);
        BioRewriteResponse result = parseRewrittenBios(response, request.getBio(), tone.getValue());
        log.info("Successfully generated {} bio rewrites", result.getRewrittenBios().size());
        return result;
    }

    /**
     * Parses the Claude API response to extract rewritten bios
     */
    private BioRewriteResponse parseRewrittenBios(String response, String originalBio, String tone) {
        List<String> rewrittenBios = new ArrayList<>();

        // Try to extract numbered versions (1. 2. 3. or 1) 2) 3))
        Pattern pattern = Pattern.compile("(?:^|\\n)\\s*\\d+[.):]\\s*(.+?)(?=\\n\\s*\\d+[.):]|$)",
                Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            String bio = matcher.group(1).trim();
            if (!bio.isEmpty() && bio.length() <= 500) {
                rewrittenBios.add(bio);
            }
        }

        // If parsing failed, try splitting by newlines
        if (rewrittenBios.isEmpty()) {
            String[] lines = response.split("\\n");
            for (String line : lines) {
                line = line.trim();
                // Skip empty lines and section headers
                if (line.length() > 20 && line.length() <= 500
                        && !line.toLowerCase().contains("version")
                        && !line.toLowerCase().contains("option")) {
                    rewrittenBios.add(line);
                    if (rewrittenBios.size() >= 3) break;
                }
            }
        }

        // Fallback: if still no results, return the response as-is
        if (rewrittenBios.isEmpty()) {
            rewrittenBios.add(response.trim());
        }

        return new BioRewriteResponse(originalBio, rewrittenBios, tone);
    }
}
