package com.groupf.dating.controller;

import com.groupf.dating.dto.*;
import com.groupf.dating.service.BioService;
import com.groupf.dating.service.ConversationStarterService;
import com.groupf.dating.service.PhotoRankingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileOptimizationController {

    private final BioService bioService;
    private final PhotoRankingService photoRankingService;
    private final ConversationStarterService conversationStarterService;

    /**
     * Rewrites a bio into multiple versions
     * POST /api/profile/rewrite-bio
     * Request body example:
     * {
     *   "bio": "I love hiking, traveling, and coffee. Software engineer by day.",
     *   "tone": "casual"  // casual, professional, bold
     * }
     * Get test data from:
     * - GET /api/test/bio-rewrite-casual
     * - GET /api/test/bio-rewrite-professional
     * - GET /api/test/bio-rewrite-bold
     * Response example:
     * {
     *   "originalBio": "Original bio text",
     *   "rewrittenBios": ["Rewritten version 1", "Rewritten version 2", "Rewritten version 3"],
     *   "tone": "casual"
     * }
     *
     * @param request Bio rewrite request (bio required, 10-500 chars; tone optional, defaults to casual)
     * @return Response containing 3 rewritten versions
     */
    @PostMapping("/rewrite-bio")
    public ResponseEntity<BioRewriteResponse> rewriteBio(
            @Valid @RequestBody BioRewriteRequest request) {
        log.info("Received bio rewrite request with tone: {}", request.getTone());

        BioRewriteResponse response = bioService.rewriteBio(request);
        log.info("Bio rewrite completed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Ranks photos for dating profile
     * POST /api/profile/rank-photos
     * Content-Type: multipart/form-data
     * Testing instructions:
     * 1. Select "Multipart" type in Fast Request
     * 2. Add parameter named "photos", select type "File[]"
     * 3. Upload 1-5 photos (JPEG/PNG format, each <10MB)
     * Response example:
     * {
     *   "rankedPhotos": [
     *     {
     *       "photoName": "photo1.jpg",
     *       "rank": 1,
     *       "score": 92.5,
     *       "reasoning": "Excellent lighting and composition, genuine natural smile...",
     *       "base64Image": null
     *     }
     *   ]
     * }
     *
     * @param photos Array of uploaded photos (1-5 photos, JPEG/PNG, each <10MB)
     * @return Ranking results, sorted by rank in ascending order
     */
    @PostMapping(value = "/rank-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoRankResponse> rankPhotos(
            @RequestParam("photos") MultipartFile[] photos) {
        log.info("Received photo ranking request with {} photos", photos.length);

        PhotoRankResponse response = photoRankingService.rankPhotos(photos);
        log.info("Photo ranking completed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Generates conversation starters based on a bio
     * POST /api/profile/generate-openers
     * Request body example:
     * {
     *   "bio": "Data scientist who loves hiking, guitar, and Italian food.",
     *   "tone": "polite"  // bold, polite, concise
     * }
     * Get test data from:
     * - GET /api/test/conversation-starters-polite
     * - GET /api/test/conversation-starters-bold
     * - GET /api/test/conversation-starters-concise
     * Response example:
     * {
     *   "bio": "Original bio text",
     *   "starters": [
     *     "I noticed you love hiking! What's your favorite trail?",
     *     "Fellow guitar player here! What kind of music do you play?",
     *     "Your bio mentions Italian food - any restaurant recommendations?"
     *   ],
     *   "tone": "polite"
     * }
     *
     * @param request Conversation starter generation request (bio required, 10-500 chars; tone optional, defaults to polite)
     * @return Response containing 3-5 conversation starters
     */
    @PostMapping("/generate-openers")
    public ResponseEntity<ConversationStarterResponse> generateOpeners(
            @Valid @RequestBody ConversationStarterRequest request) {
        log.info("Received conversation starter request with tone: {}", request.getTone());

        ConversationStarterResponse response = conversationStarterService.generateStarters(request);
        log.info("Conversation starters generated successfully");
        return ResponseEntity.ok(response);
    }
}
