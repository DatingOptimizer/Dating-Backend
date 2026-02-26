package com.groupf.dating.service.impl;

import com.groupf.dating.common.ToneType;
import com.groupf.dating.dto.ConversationStarterRequest;
import com.groupf.dating.dto.ConversationStarterResponse;
import com.groupf.dating.service.ClaudeApiService;
import com.groupf.dating.service.ConversationStarterService;
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
public class ConversationStarterServiceImpl implements ConversationStarterService {

    private final ClaudeApiService claudeApiService;

    @Override
    public ConversationStarterResponse generateStarters(ConversationStarterRequest request) {
        // Validate input
        String validationError = ValidationUtil.getBioValidationError(request.getBio());
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        ToneType tone = ToneType.fromString(request.getTone());

        String systemPrompt = PromptBuilder.buildConversationStarterSystemPrompt(tone);
        String userPrompt = PromptBuilder.buildConversationStarterUserPrompt(request.getBio());

        log.info("Generating conversation starters with tone: {}", tone.getValue());

        String response = claudeApiService.callClaudeApi(systemPrompt, userPrompt);
        ConversationStarterResponse result = parseConversationStarters(response, request.getBio(), tone.getValue());
        log.info("Successfully generated {} conversation starters", result.getStarters().size());
        return result;
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
                if (!line.isEmpty() && line.length() > 10
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
