package com.groupf.dating.util;

import com.groupf.dating.common.AppConstants;
import com.groupf.dating.common.ToneType;

public class PromptBuilder {

    private PromptBuilder() {
        // Prevent instantiation
    }

    /**
     * Builds system prompt for bio rewriting
     */
    public static String buildBioRewriteSystemPrompt(ToneType tone) {
        return String.format(
            "You are an expert dating profile writer. Your task is to rewrite dating profile bios to make them more engaging and attractive. " +
            "The tone should be %s: %s. " +
            "Generate exactly %d alternative versions. " +
            "Keep each version under %d characters. " +
            "Focus on highlighting personality, interests, and what makes the person unique. " +
            "Avoid clichés and be authentic.",
            tone.getValue(),
            tone.getDescription(),
            AppConstants.BIO_REWRITE_COUNT,
            AppConstants.BIO_MAX_LENGTH
        );
    }

    /**
     * Builds user prompt for bio rewriting
     */
    public static String buildBioRewriteUserPrompt(String originalBio) {
        return String.format(
            "Please rewrite this dating profile bio into %d different versions:\n\n\"%s\"\n\n" +
            "Return only the %d rewritten versions, numbered 1-3, without any additional explanation.",
            AppConstants.BIO_REWRITE_COUNT,
            originalBio,
            AppConstants.BIO_REWRITE_COUNT
        );
    }

    /**
     * Builds system prompt for photo ranking
     */
    public static String buildPhotoRankingSystemPrompt() {
        return "You are an expert at analyzing dating profile photos. " +
               "Your task is to rank photos based on their effectiveness for dating profiles. " +
               "Consider factors like: lighting, composition, facial expression, context, authenticity, and overall appeal. " +
               "Provide a ranking with a score (0-100) and clear reasoning for each photo.";
    }

    /**
     * Builds user prompt for photo ranking
     */
    public static String buildPhotoRankingUserPrompt(int photoCount) {
        return String.format(
            "I'm providing %d photos for my dating profile. " +
            "Please rank them from best to worst for use in a dating profile. " +
            "For each photo, provide:\n" +
            "1. Rank (1 being best)\n" +
            "2. Score (0-100)\n" +
            "3. Brief reasoning (2-3 sentences)\n\n" +
            "Format your response as a JSON array with objects containing: rank, score, and reasoning fields.",
            photoCount
        );
    }

    /**
     * Builds system prompt for conversation starters
     */
    public static String buildConversationStarterSystemPrompt(ToneType tone) {
        return String.format(
            "You are an expert at creating engaging conversation starters for dating apps. " +
            "Your tone should be %s: %s. " +
            "Generate personalized opening messages that reference specific details from the person's bio. " +
            "Avoid generic openers like 'Hey' or 'What's up'. " +
            "Make them creative, genuine, and likely to get a response.",
            tone.getValue(),
            tone.getDescription()
        );
    }

    /**
     * Builds user prompt for conversation starters
     */
    public static String buildConversationStarterUserPrompt(String bio) {
        return String.format(
            "Based on this dating profile bio, generate %d-%d engaging conversation starters:\n\n\"%s\"\n\n" +
            "Each opener should reference something specific from the bio and invite a response. " +
            "Return only the conversation starters, numbered, without additional explanation.",
            AppConstants.CONVERSATION_STARTER_MIN_COUNT,
            AppConstants.CONVERSATION_STARTER_MAX_COUNT,
            bio
        );
    }
}
