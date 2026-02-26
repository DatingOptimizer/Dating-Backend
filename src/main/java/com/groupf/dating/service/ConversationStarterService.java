package com.groupf.dating.service;

import com.groupf.dating.dto.ConversationStarterRequest;
import com.groupf.dating.dto.ConversationStarterResponse;

/**
 * Service interface for conversation starter generation functionality
 */
public interface ConversationStarterService {

    /**
     * Generates conversation starters based on a bio
     *
     * @param request the conversation starter request containing the bio and tone preference
     * @return the conversation starter response with multiple starters
     */
    ConversationStarterResponse generateStarters(ConversationStarterRequest request);
}
