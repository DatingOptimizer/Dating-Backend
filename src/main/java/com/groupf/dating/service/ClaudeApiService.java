package com.groupf.dating.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.groupf.dating.common.ApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeApiService {

    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper;

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    /**
     * Calls Claude API with text-only messages
     */
    public Mono<String> callClaudeApi(String systemPrompt, String userPrompt) {
        try {
            ObjectNode requestBody = buildTextRequest(systemPrompt, userPrompt);

            log.debug("Calling Claude API with text request");

            return claudeWebClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(this::extractTextResponse)
                    .doOnSuccess(response -> log.debug("Claude API call successful"))
                    .doOnError(error -> log.error("Claude API call failed", error))
                    .onErrorResume(this::handleApiError);
        } catch (Exception e) {
            log.error("Error building Claude API request", e);
            return Mono.error(new RuntimeException("Failed to build API request", e));
        }
    }

    /**
     * Calls Claude API with vision (images + text)
     */
    public Mono<String> callClaudeApiWithVision(String systemPrompt, String userPrompt,
                                                 List<ImageContent> images) {
        try {
            ObjectNode requestBody = buildVisionRequest(systemPrompt, userPrompt, images);

            log.debug("Calling Claude API with vision request ({} images)", images.size());

            return claudeWebClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(this::extractTextResponse)
                    .doOnSuccess(response -> log.debug("Claude Vision API call successful"))
                    .doOnError(error -> log.error("Claude Vision API call failed", error))
                    .onErrorResume(this::handleApiError);
        } catch (Exception e) {
            log.error("Error building Claude Vision API request", e);
            return Mono.error(new RuntimeException("Failed to build API request", e));
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
    private Mono<String> handleApiError(Throwable error) {
        log.error("Claude API error: {}", error.getMessage());

        if (error.getMessage() != null && error.getMessage().contains("429")) {
            return Mono.error(new RuntimeException("Rate limit exceeded. Please try again later."));
        } else if (error.getMessage() != null && error.getMessage().contains("401")) {
            return Mono.error(new RuntimeException("Invalid API key"));
        } else if (error.getMessage() != null && error.getMessage().contains("timeout")) {
            return Mono.error(new RuntimeException("Request timeout. Please try again."));
        }

        return Mono.error(new RuntimeException("Claude API error: " + error.getMessage()));
    }

    /**
     * Image content holder for vision API
     */
    public static class ImageContent {
        private final String base64Data;
        private final String mediaType;

        public ImageContent(String base64Data, String mediaType) {
            this.base64Data = base64Data;
            this.mediaType = mediaType;
        }

        public String getBase64Data() {
            return base64Data;
        }

        public String getMediaType() {
            return mediaType;
        }
    }
}
