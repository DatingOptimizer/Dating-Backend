package com.groupf.dating.controller;

import com.groupf.dating.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller - For Fast Request plugin testing
 * Provides sample request bodies for all APIs
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    /**
     * Get test data for Bio rewrite - Casual tone
     * GET /api/test/bio-rewrite-casual
     *
     * Example response:
     * {
     *   "bio": "I love hiking and coffee",
     *   "tone": "casual"
     * }
     */
    @GetMapping("/bio-rewrite-casual")
    public ResponseEntity<BioRewriteRequest> getBioRewriteCasualExample() {
        BioRewriteRequest request = new BioRewriteRequest(
            "I love hiking, traveling, and trying new coffee shops. Software engineer by day, adventure seeker by weekend.",
            "casual"
        );
        return ResponseEntity.ok(request);
    }

    /**
     * Get test data for Bio rewrite - Professional tone
     * GET /api/test/bio-rewrite-professional
     */
    @GetMapping("/bio-rewrite-professional")
    public ResponseEntity<BioRewriteRequest> getBioRewriteProfessionalExample() {
        BioRewriteRequest request = new BioRewriteRequest(
            "Passionate about technology and innovation. Love exploring new cultures through travel and cuisine.",
            "professional"
        );
        return ResponseEntity.ok(request);
    }

    /**
     * Get test data for Bio rewrite - Bold tone
     * GET /api/test/bio-rewrite-bold
     */
    @GetMapping("/bio-rewrite-bold")
    public ResponseEntity<BioRewriteRequest> getBioRewriteBoldExample() {
        BioRewriteRequest request = new BioRewriteRequest(
            "Adrenaline junkie who loves skydiving, rock climbing, and spontaneous road trips. Let's create some epic stories together.",
            "bold"
        );
        return ResponseEntity.ok(request);
    }

    /**
     * Get test data for conversation starters - Polite tone
     * GET /api/test/conversation-starters-polite
     */
    @GetMapping("/conversation-starters-polite")
    public ResponseEntity<ConversationStarterRequest> getConversationStarterPoliteExample() {
        ConversationStarterRequest request = new ConversationStarterRequest(
            "I'm a data scientist who loves hiking in the mountains, playing guitar, and cooking Italian food. Always looking for the next adventure.",
            "polite"
        );
        return ResponseEntity.ok(request);
    }

    /**
     * Get test data for conversation starters - Bold tone
     * GET /api/test/conversation-starters-bold
     */
    @GetMapping("/conversation-starters-bold")
    public ResponseEntity<ConversationStarterRequest> getConversationStarterBoldExample() {
        ConversationStarterRequest request = new ConversationStarterRequest(
            "Marathon runner, sushi enthusiast, and amateur photographer. I believe the best conversations happen over good food.",
            "bold"
        );
        return ResponseEntity.ok(request);
    }

    /**
     * Get test data for conversation starters - Concise tone
     * GET /api/test/conversation-starters-concise
     */
    @GetMapping("/conversation-starters-concise")
    public ResponseEntity<ConversationStarterRequest> getConversationStarterConciseExample() {
        ConversationStarterRequest request = new ConversationStarterRequest(
            "Dog lover, yoga instructor, plant parent. Weekend warrior who's equally happy at a concert or a quiet bookstore.",
            "concise"
        );
        return ResponseEntity.ok(request);
    }

    /**
     * Get descriptions of all test scenarios
     * GET /api/test/scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> getTestScenarios() {
        Map<String, Object> scenarios = new HashMap<>();

        // Bio rewrite scenarios
        Map<String, String> bioScenarios = new HashMap<>();
        bioScenarios.put("1. Casual Tone", "POST /api/profile/rewrite-bio - Use response from /api/test/bio-rewrite-casual");
        bioScenarios.put("2. Professional Tone", "POST /api/profile/rewrite-bio - Use response from /api/test/bio-rewrite-professional");
        bioScenarios.put("3. Bold Tone", "POST /api/profile/rewrite-bio - Use response from /api/test/bio-rewrite-bold");
        scenarios.put("Bio Rewrite Tests", bioScenarios);

        // Conversation starter scenarios
        Map<String, String> starterScenarios = new HashMap<>();
        starterScenarios.put("1. Polite Tone", "POST /api/profile/generate-openers - Use response from /api/test/conversation-starters-polite");
        starterScenarios.put("2. Bold Tone", "POST /api/profile/generate-openers - Use response from /api/test/conversation-starters-bold");
        starterScenarios.put("3. Concise Tone", "POST /api/profile/generate-openers - Use response from /api/test/conversation-starters-concise");
        scenarios.put("Conversation Starter Tests", starterScenarios);

        // Photo ranking scenarios
        Map<String, String> photoScenarios = new HashMap<>();
        photoScenarios.put("1. Single Photo", "POST /api/profile/rank-photos - Upload 1 photo (JPEG/PNG, <10MB)");
        photoScenarios.put("2. Multiple Photos", "POST /api/profile/rank-photos - Upload 2-5 photos");
        photoScenarios.put("3. Maximum Count", "POST /api/profile/rank-photos - Upload 5 photos (max limit)");
        scenarios.put("Photo Ranking Tests", photoScenarios);

        // Error handling scenarios
        Map<String, String> errorScenarios = new HashMap<>();
        errorScenarios.put("1. Bio Too Long", "POST /api/profile/rewrite-bio - bio field exceeds 500 characters");
        errorScenarios.put("2. Bio Too Short", "POST /api/profile/rewrite-bio - bio field less than 10 characters");
        errorScenarios.put("3. Too Many Photos", "POST /api/profile/rank-photos - Upload more than 5 photos");
        errorScenarios.put("4. Invalid Image Format", "POST /api/profile/rank-photos - Upload non-JPEG/PNG format");
        scenarios.put("Error Handling Tests", errorScenarios);

        scenarios.put("Note", "When testing with Fast Request plugin, ensure CLAUDE_API_KEY environment variable is set");

        return ResponseEntity.ok(scenarios);
    }

    /**
     * Quick health check for all APIs
     * GET /api/test/quick-check
     */
    @GetMapping("/quick-check")
    public ResponseEntity<Map<String, Object>> quickCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "OK");
        status.put("message", "Test controller is running properly");
        status.put("availableEndpoints", Map.of(
            "health", "GET /api/health",
            "bioRewrite", "POST /api/profile/rewrite-bio",
            "rankPhotos", "POST /api/profile/rank-photos",
            "generateOpeners", "POST /api/profile/generate-openers"
        ));
        status.put("testDataEndpoints", Map.of(
            "bioCasual", "GET /api/test/bio-rewrite-casual",
            "bioProfessional", "GET /api/test/bio-rewrite-professional",
            "bioBold", "GET /api/test/bio-rewrite-bold",
            "startersPolite", "GET /api/test/conversation-starters-polite",
            "startersBold", "GET /api/test/conversation-starters-bold",
            "startersConcise", "GET /api/test/conversation-starters-concise"
        ));
        return ResponseEntity.ok(status);
    }
}
