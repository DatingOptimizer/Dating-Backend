package com.groupf.dating.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupf.dating.exception.ClaudeApiException;
import com.groupf.dating.exception.ErrorCode;
import com.groupf.dating.service.impl.ClaudeApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaudeApiServiceImplTest {

    @Mock
    private RestClient claudeRestClient;

    private ClaudeApiServiceImpl claudeApiService;

    @BeforeEach
    void setUp() {
        // Use a no-retry template so tests run without delay
        RetryTemplate noRetry = new RetryTemplate();
        noRetry.setRetryPolicy(new SimpleRetryPolicy(1));

        claudeApiService = new ClaudeApiServiceImpl(claudeRestClient, new ObjectMapper(), noRetry);
        ReflectionTestUtils.setField(claudeApiService, "model", "claude-test-model");
        ReflectionTestUtils.setField(claudeApiService, "maxTokens", 1024);
    }

    // ──────────── fromHttpStatus error mapping ────────────

    @Test
    void fromHttpStatus_401_mapsToApiKeyInvalid() {
        ClaudeApiException ex = ClaudeApiException.fromHttpStatus(401, "Unauthorized");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CLAUDE_API_KEY_INVALID);
        assertThat(ex.isRetryable()).isFalse();
    }

    @Test
    void fromHttpStatus_429_mapsToRateLimit_andIsRetryable() {
        ClaudeApiException ex = ClaudeApiException.fromHttpStatus(429, "Rate limited");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CLAUDE_RATE_LIMIT);
        assertThat(ex.isRetryable()).isTrue();
    }

    @Test
    void fromHttpStatus_503_mapsToOverloaded_andIsRetryable() {
        ClaudeApiException ex = ClaudeApiException.fromHttpStatus(503, "Overloaded");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CLAUDE_OVERLOADED);
        assertThat(ex.isRetryable()).isTrue();
    }

    @Test
    void fromHttpStatus_500_mapsToServerError_andIsNotRetryable() {
        ClaudeApiException ex = ClaudeApiException.fromHttpStatus(500, "Server error");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CLAUDE_SERVER_ERROR);
        assertThat(ex.isRetryable()).isFalse();
    }

    @Test
    void fromHttpStatus_400_mapsToInvalidRequest_andIsNotRetryable() {
        ClaudeApiException ex = ClaudeApiException.fromHttpStatus(400, "Bad request");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CLAUDE_INVALID_REQUEST);
        assertThat(ex.isRetryable()).isFalse();
    }

    // ──────────── ErrorCode retryable logic ────────────

    @Test
    void errorCode_timeout_isRetryable() {
        assertThat(ErrorCode.CLAUDE_TIMEOUT.isRetryable()).isTrue();
    }

    @Test
    void errorCode_connectionError_isRetryable() {
        assertThat(ErrorCode.CLAUDE_CONNECTION_ERROR.isRetryable()).isTrue();
    }

    @Test
    void errorCode_invalidRequest_isNotRetryable() {
        assertThat(ErrorCode.CLAUDE_INVALID_REQUEST.isRetryable()).isFalse();
    }

    @Test
    void errorCode_apiKeyInvalid_isNotRetryable() {
        assertThat(ErrorCode.CLAUDE_API_KEY_INVALID.isRetryable()).isFalse();
    }

    // ──────────── callClaudeApi — response parsing ────────────

    @Test
    void callClaudeApi_throwsParseError_whenResponseHasNoContentField() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(claudeRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(anyString())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        // Response is valid JSON but missing the "content" field
        when(responseSpec.body(String.class)).thenReturn("{\"id\": \"msg_123\"}");

        assertThatThrownBy(() -> claudeApiService.callClaudeApi("system", "user"))
                .isInstanceOf(ClaudeApiException.class)
                .extracting(e -> ((ClaudeApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLAUDE_RESPONSE_PARSE_ERROR);
    }

    @Test
    void callClaudeApi_extractsTextFromValidResponse() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        String validResponse = """
                {
                  "content": [{"type": "text", "text": "Rewritten bio here"}],
                  "stop_reason": "end_turn"
                }""";

        when(claudeRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(anyString())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(validResponse);

        String result = claudeApiService.callClaudeApi("system", "user");

        assertThat(result).isEqualTo("Rewritten bio here");
    }

    // ====== HANDWRITTEN TESTS ======

    @Test
    void fromHttpStatus_404_fallsThroughToConnectionError() {
        // 404 isn't explicitly handled, so it hits the default case -> CLAUDE_CONNECTION_ERROR
        ClaudeApiException ex = ClaudeApiException.fromHttpStatus(404, "not found");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CLAUDE_CONNECTION_ERROR);
        assertThat(ex.isRetryable()).isTrue(); // connection errors are retryable
    }

    @Test
    void errorCode_overloaded_isRetryable() {
        // overloaded is transient, should be retryable
        assertThat(ErrorCode.CLAUDE_OVERLOADED.isRetryable()).isTrue();
    }

    @Test
    void errorCode_quotaExceeded_isNotRetryable() {
        // quota errors won't go away on retry, should be non-retryable
        assertThat(ErrorCode.CLAUDE_QUOTA_EXCEEDED.isRetryable()).isFalse();
    }

    @Test
    void errorCode_fromCode_returnsCorrectEntry() {
        // spot-check the lookup by code number
        ErrorCode found = ErrorCode.fromCode(4002);
        assertThat(found).isEqualTo(ErrorCode.CLAUDE_RATE_LIMIT);
    }
}
