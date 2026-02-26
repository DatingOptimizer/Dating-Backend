package com.groupf.dating.service;

import lombok.Value;

import java.util.List;

/**
 * Service interface for Claude API integration
 */
public interface ClaudeApiService {

    /**
     * Calls Claude API with text-only messages
     *
     * @param systemPrompt the system prompt to set the context
     * @param userPrompt the user prompt with the actual request
     * @return the API response text
     */
    String callClaudeApi(String systemPrompt, String userPrompt);

    /**
     * Calls Claude API with vision (images + text)
     *
     * @param systemPrompt the system prompt to set the context
     * @param userPrompt the user prompt with the actual request
     * @param images list of images to analyze
     * @return the API response text
     */
    String callClaudeApiWithVision(String systemPrompt, String userPrompt, List<ImageContent> images);

    /**
     * Image content holder for vision API
     */
    @Value
    class ImageContent {
        String base64Data;
        String mediaType; // e.g., "image/jpeg"
    }
}
