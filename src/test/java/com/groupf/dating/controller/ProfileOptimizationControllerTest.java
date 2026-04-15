package com.groupf.dating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupf.dating.dto.BioRewriteRequest;
import com.groupf.dating.dto.BioRewriteResponse;
import com.groupf.dating.dto.ConversationStarterRequest;
import com.groupf.dating.dto.ConversationStarterResponse;
import com.groupf.dating.exception.ClaudeApiException;
import com.groupf.dating.exception.ErrorCode;
import com.groupf.dating.exception.GlobalExceptionHandler;
import com.groupf.dating.service.BioService;
import com.groupf.dating.service.ConversationStarterService;
import com.groupf.dating.service.PhotoRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileOptimizationControllerTest {

    @Mock
    private BioService bioService;

    @Mock
    private ConversationStarterService conversationStarterService;

    @Mock
    private PhotoRankingService photoRankingService;

    @InjectMocks
    private ProfileOptimizationController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ──────────── POST /api/profile/rewrite-bio ────────────

    @Test
    void rewriteBio_validRequest_returns200WithResponse() throws Exception {
        BioRewriteRequest request = new BioRewriteRequest("I love hiking and I'm a software engineer.", "casual");
        BioRewriteResponse response = new BioRewriteResponse(request.getBio(),
                List.of("Version 1", "Version 2", "Version 3"), "casual");

        when(bioService.rewriteBio(any())).thenReturn(response);

        mockMvc.perform(post("/api/profile/rewrite-bio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalBio").value(request.getBio()))
                .andExpect(jsonPath("$.tone").value("casual"))
                .andExpect(jsonPath("$.rewrittenBios").isArray())
                .andExpect(jsonPath("$.rewrittenBios.length()").value(3));
    }

    @Test
    void rewriteBio_bioTooShort_returns400() throws Exception {
        BioRewriteRequest request = new BioRewriteRequest("Short", "casual"); // < 10 chars

        mockMvc.perform(post("/api/profile/rewrite-bio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rewriteBio_emptyBio_returns400() throws Exception {
        BioRewriteRequest request = new BioRewriteRequest("", "casual");

        mockMvc.perform(post("/api/profile/rewrite-bio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rewriteBio_claudeApiRateLimit_returns429() throws Exception {
        when(bioService.rewriteBio(any()))
                .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_RATE_LIMIT));

        BioRewriteRequest request = new BioRewriteRequest("I love hiking and I'm a software engineer.", "casual");

        mockMvc.perform(post("/api/profile/rewrite-bio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    void rewriteBio_invalidTone_returns400() throws Exception {
        when(bioService.rewriteBio(any()))
                .thenThrow(new IllegalArgumentException("invalidtone is not a valid ToneType"));

        BioRewriteRequest request = new BioRewriteRequest("I love hiking and I'm a software engineer.", "invalidtone");

        mockMvc.perform(post("/api/profile/rewrite-bio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ──────────── POST /api/profile/generate-openers ────────────

    @Test
    void generateOpeners_validRequest_returns200() throws Exception {
        ConversationStarterRequest request = new ConversationStarterRequest(
                "Data scientist who loves hiking and guitar.", "polite");
        ConversationStarterResponse response = new ConversationStarterResponse(
                request.getBio(), List.of("Starter 1", "Starter 2", "Starter 3"), "polite");

        when(conversationStarterService.generateStarters(any())).thenReturn(response);

        mockMvc.perform(post("/api/profile/generate-openers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starters").isArray());
    }

    @Test
    void generateOpeners_bioTooShort_returns400() throws Exception {
        ConversationStarterRequest request = new ConversationStarterRequest("Short", "polite");

        mockMvc.perform(post("/api/profile/generate-openers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ====== HANDWRITTEN TESTS ======

    @Test
    void rewriteBio_nullTone_isStillAccepted() throws Exception {
        // null tone should default to casual, not fail at the controller layer
        BioRewriteRequest req = new BioRewriteRequest("I love hiking and I'm a software engineer.", null);
        BioRewriteResponse resp = new BioRewriteResponse(req.getBio(),
                List.of("v1", "v2", "v3"), "casual");
        when(bioService.rewriteBio(any())).thenReturn(resp);

        mockMvc.perform(post("/api/profile/rewrite-bio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").value("casual"));
    }

    @Test
    void generateOpeners_validRequest_responseHasCorrectBioEchoed() throws Exception {
        // checking the bio in the request is reflected back in the response
        String bio = "Data scientist who loves hiking and guitar.";
        ConversationStarterRequest req = new ConversationStarterRequest(bio, "casual");
        ConversationStarterResponse resp = new ConversationStarterResponse(bio, List.of("opener1"), "casual");

        when(conversationStarterService.generateStarters(any())).thenReturn(resp);

        mockMvc.perform(post("/api/profile/generate-openers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value(bio));
    }

    @Test
    void rewriteBio_claudeServerError_returns500() throws Exception {
        // make sure 500 from claude propagates properly, not swallowed as 400
        when(bioService.rewriteBio(any()))
                .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_SERVER_ERROR));

        BioRewriteRequest req = new BioRewriteRequest("I love hiking and I'm a software engineer.", "casual");

        mockMvc.perform(post("/api/profile/rewrite-bio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }
}
