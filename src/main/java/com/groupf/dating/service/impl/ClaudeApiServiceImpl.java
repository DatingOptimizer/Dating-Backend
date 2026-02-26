package com.groupf.dating.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.groupf.dating.common.ApiConstants;
import com.groupf.dating.service.ClaudeApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeApiServiceImpl implements ClaudeApiService {

    private final RestClient claudeRestClient;
    private final ObjectMapper objectMapper;

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    @Override
    public String callClaudeApi(String systemPrompt, String userPrompt) {
        ObjectNode requestBody = buildTextRequest(systemPrompt, userPrompt);

        log.debug("Calling Claude API with text request");

        try {
            JsonNode response = claudeRestClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String result = extractTextResponse(response);
            log.debug("Claude API call successful");
            return result;
        } catch (RestClientException e) {
            log.error("Claude API call failed", e);
            throw handleApiError(e);
        }
    }

    @Override
    public String callClaudeApiWithVision(String systemPrompt, String userPrompt,
                                           List<ImageContent> images) {
        ObjectNode requestBody = buildVisionRequest(systemPrompt, userPrompt, images);

        log.debug("Calling Claude API with vision request ({} images)", images.size());

        try {
            JsonNode response = claudeRestClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String result = extractTextResponse(response);
            log.debug("Claude Vision API call successful");
            return result;
        } catch (RestClientException e) {
            log.error("Claude Vision API call failed", e);
            throw handleApiError(e);
        }
    }

    /**
     * Builds request body for text-only messages
     */
    private ObjectNode buildTextRequest(String systemPrompt, String userPrompt) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("max_tokens", maxTokens);
        request.put("temperature", ApiConstants.TEMPERATURE);

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            request.put("system", systemPrompt);
        }

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        request.set("messages", messages);

        return request;
    }

    /**
     * Builds request body for vision messages (with images)
     */
    private ObjectNode buildVisionRequest(String systemPrompt, String userPrompt,
                                          List<ImageContent> images) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("max_tokens", maxTokens);
        request.put("temperature", ApiConstants.TEMPERATURE);

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            request.put("system", systemPrompt);
        }

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");

        // Build content array with images and text
        ArrayNode contentArray = objectMapper.createArrayNode();

        // Add images first
        for (ImageContent image : images) {
            ObjectNode imageContent = objectMapper.createObjectNode();
            imageContent.put("type", "image");

            ObjectNode source = objectMapper.createObjectNode();
            source.put("type", "base64");
            source.put("media_type", image.getMediaType());
            source.put("data", image.getBase64Data());

            imageContent.set("source", source);
            contentArray.add(imageContent);
        }

        // Add text prompt
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", userPrompt);
        contentArray.add(textContent);

        userMessage.set("content", contentArray);
        messages.add(userMessage);

        request.set("messages", messages);

        return request;
    }

    /**
     * Extracts text response from Claude API response
     */
    private String extractTextResponse(JsonNode response) {
        try {
            JsonNode content = response.get("content");
            if (content != null && content.isArray() && content.size() > 0) {
                JsonNode firstContent = content.get(0);
                if (firstContent.has("text")) {
                    return firstContent.get("text").asText();
                }
            }

            log.error("Unexpected response format from Claude API: {}", response);
            throw new RuntimeException("Failed to extract text from API response");
        } catch (Exception e) {
            log.error("Error parsing Claude API response", e);
            throw new RuntimeException("Failed to parse API response", e);
        }
    }

    /**
     * Handles API errors
     */
    private RuntimeException handleApiError(Throwable error) {
        log.error("Claude API error: {}", error.getMessage());

        String message = error.getMessage();
        if (message != null && message.contains("429")) {
            return new RuntimeException("Rate limit exceeded. Please try again later.");
        } else if (message != null && message.contains("401")) {
            return new RuntimeException("Invalid API key");
        } else if (message != null && message.contains("timeout")) {
            return new RuntimeException("Request timeout. Please try again.");
        }

        return new RuntimeException("Claude API error: " + message);
    }
}
