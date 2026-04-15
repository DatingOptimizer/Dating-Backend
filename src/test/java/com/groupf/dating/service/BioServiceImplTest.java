package com.groupf.dating.service;

import com.groupf.dating.dto.BioRewriteRequest;
import com.groupf.dating.dto.BioRewriteResponse;
import com.groupf.dating.repository.ProfileRequestRepository;
import com.groupf.dating.service.impl.BioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BioServiceImplTest {

    @Mock
    private ClaudeApiService claudeApiService;

    @Mock
    private ProfileRequestRepository profileRequestRepository;

    @InjectMocks
    private BioServiceImpl bioService;

    @BeforeEach
    void setUpSecurityContext() {
        // Simulate an authenticated user in the security context
        var auth = new UsernamePasswordAuthenticationToken("user-123", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void rewriteBio_numberedFormat_parsesThreeVersions() {
        String claudeResponse = """
                1. I'm a software engineer who spends weekends hiking and chasing sunsets.
                2. Tech by day, trail runner by night — always up for a good coffee chat.
                3. Coding and hiking: two ways I like to solve complex problems.""";

        when(claudeApiService.callClaudeApi(anyString(), anyString())).thenReturn(claudeResponse);

        BioRewriteRequest request = new BioRewriteRequest("I love hiking and I'm a software engineer.", "casual");
        BioRewriteResponse response = bioService.rewriteBio(request);

        assertThat(response.getOriginalBio()).isEqualTo(request.getBio());
        assertThat(response.getTone()).isEqualTo("casual");
        assertThat(response.getRewrittenBios()).hasSize(3);
        assertThat(response.getRewrittenBios().get(0)).contains("software engineer");
    }

    @Test
    void rewriteBio_fallbackToRawResponse_whenParsingYieldsNothing() {
        String claudeResponse = "Here is your rewritten bio in one short version.";
        when(claudeApiService.callClaudeApi(anyString(), anyString())).thenReturn(claudeResponse);

        BioRewriteRequest request = new BioRewriteRequest("I love hiking and I'm a software engineer.", "bold");
        BioRewriteResponse response = bioService.rewriteBio(request);

        assertThat(response.getRewrittenBios()).hasSize(1);
        assertThat(response.getRewrittenBios().get(0)).isEqualTo(claudeResponse.trim());
    }

    @Test
    void rewriteBio_defaultsToCasualTone_whenToneIsNull() {
        String claudeResponse = "1. Version one.\n2. Version two.\n3. Version three — casual and friendly!";
        when(claudeApiService.callClaudeApi(anyString(), anyString())).thenReturn(claudeResponse);

        BioRewriteRequest request = new BioRewriteRequest("I love hiking and I'm a software engineer.", null);
        BioRewriteResponse response = bioService.rewriteBio(request);

        assertThat(response.getTone()).isEqualTo("casual");
    }

    @Test
    void rewriteBio_throwsIllegalArgument_forInvalidTone() {
        BioRewriteRequest request = new BioRewriteRequest("I love hiking.", "invalidtone");

        assertThatThrownBy(() -> bioService.rewriteBio(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalidtone");
    }

    @Test
    void rewriteBio_stillReturnsResult_whenDatabaseSaveFails() {
        String claudeResponse = "1. Bio one.\n2. Bio two.\n3. Bio three.";
        when(claudeApiService.callClaudeApi(anyString(), anyString())).thenReturn(claudeResponse);
        when(profileRequestRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        BioRewriteRequest request = new BioRewriteRequest("I love hiking and coding.", "warm");

        // Should not throw — DB failure is swallowed gracefully
        BioRewriteResponse response = bioService.rewriteBio(request);
        assertThat(response.getRewrittenBios()).isNotEmpty();
    }

    @Test
    void rewriteBio_trimsBioWhitespace_beforeProcessing() {
        String claudeResponse = "1. Bio one.\n2. Bio two.\n3. Bio three.";
        when(claudeApiService.callClaudeApi(anyString(), anyString())).thenReturn(claudeResponse);

        BioRewriteRequest request = new BioRewriteRequest("  I love hiking and coding.  ", "casual");
        BioRewriteResponse response = bioService.rewriteBio(request);

        assertThat(response.getOriginalBio()).isEqualTo("I love hiking and coding.");
    }

    @Test
    void rewriteBio_savesToDatabaseWithCorrectUserId() {
        String claudeResponse = "1. One.\n2. Two.\n3. Three.";
        when(claudeApiService.callClaudeApi(anyString(), anyString())).thenReturn(claudeResponse);

        BioRewriteRequest request = new BioRewriteRequest("I love hiking and coding.", "polite");
        bioService.rewriteBio(request);

        verify(profileRequestRepository).save(argThat(entity ->
                "user-123".equals(entity.getUserId()) &&
                "polite".equals(entity.getTonePreference())
        ));
    }

    // ====== HANDWRITTEN TESTS ======

    @Test
    void rewriteBio_humorous_tone_works() {
        // make sure humorous is accepted, it's one of the valid tones
        String claudeResponse = "1. haha bio one.\n2. funny bio two.\n3. lol bio three.";
        when(claudeApiService.callClaudeApi(anyString(), anyString())).thenReturn(claudeResponse);

        var req = new BioRewriteRequest("I like to joke around and make people laugh.", "humorous");
        var result = bioService.rewriteBio(req);

        assertThat(result.getTone()).isEqualTo("humorous");
        assertThat(result.getRewrittenBios()).hasSize(3);
    }

    @Test
    void rewriteBio_warm_tone_works() {
        // every valid tone should pass through without blowing up
        String claudeResponse = "1. warm bio one.\n2. warm bio two.\n3. warm bio three.";
        when(claudeApiService.callClaudeApi(anyString(), anyString())).thenReturn(claudeResponse);

        var req = new BioRewriteRequest("I genuinely care about the people in my life.", "warm");
        var result = bioService.rewriteBio(req);

        assertThat(result.getTone()).isEqualTo("warm");
    }

    @Test
    void rewriteBio_originalBioIsPreservedInResponse() {
        // make sure the original is echoed back, not accidentally replaced
        String originalBio = "I brew my own beer and love board games.";
        when(claudeApiService.callClaudeApi(anyString(), anyString()))
                .thenReturn("1. v1.\n2. v2.\n3. v3.");

        var result = bioService.rewriteBio(new BioRewriteRequest(originalBio, "bold"));

        assertThat(result.getOriginalBio()).isEqualTo(originalBio);
    }
}
