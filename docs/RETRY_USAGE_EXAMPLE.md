# Retry Mechanism Usage Examples

## Table of Contents
1. [Using RetryTemplate (Recommended)](#using-retrytemplate-recommended)
2. [Using @Retryable Annotation](#using-retryable-annotation)
3. [Manual Retry Logic](#manual-retry-logic)
4. [Testing the Retry Mechanism](#testing-the-retry-mechanism)

---

## Using RetryTemplate (Recommended)

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class ClaudeApiService {

    private final RetryTemplate claudeApiRetryTemplate;
    private final RestTemplate restTemplate;

    @Value("${claude.api.url}")
    private String claudeApiUrl;

    @Value("${claude.api.key}")
    private String apiKey;

    /**
     * Claude API call with retry
     */
    public String callClaudeApi(String prompt) {
        return claudeApiRetryTemplate.execute(context -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("x-api-key", apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> requestBody = Map.of(
                    "model", "claude-sonnet-4-6",
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 1024
                );

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                    claudeApiUrl + "/v1/messages",
                    entity,
                    Map.class
                );

                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw ClaudeApiException.fromHttpStatus(
                        response.getStatusCode().value(),
                        "API call failed"
                    );
                }

                return extractContent(response.getBody());

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw ClaudeApiException.fromHttpStatus(
                    e.getStatusCode().value(),
                    e.getMessage()
                );
            } catch (ResourceAccessException e) {
                throw new ClaudeApiException(
                    ErrorCode.CLAUDE_CONNECTION_ERROR,
                    "Failed to connect to Claude API",
                    e
                );
            }
        });
    }

    private String extractContent(Map<String, Object> responseBody) {
        // Parse response content
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
        if (content != null && !content.isEmpty()) {
            return (String) content.get(0).get("text");
        }
        throw new ClaudeApiException(ErrorCode.CLAUDE_RESPONSE_PARSE_ERROR);
    }
}
```

### Retry with Recovery Callback

```java
@Service
@RequiredArgsConstructor
public class PhotoRankingService {

    private final RetryTemplate claudeApiRetryTemplate;

    public PhotoRankingResponse rankPhotos(PhotoRankingRequest request) {
        return claudeApiRetryTemplate.execute(
            // Main logic
            context -> {
                log.info("Ranking photos, attempt: {}", context.getRetryCount() + 1);
                return callClaudeApiForRanking(request);
            },
            // Recovery logic after retries exhausted
            context -> {
                log.error("Photo ranking failed after {} attempts", context.getRetryCount());
                throw new BusinessException(
                    ErrorCode.PHOTO_ANALYSIS_FAILED,
                    "Failed to rank photos after multiple attempts"
                );
            }
        );
    }

    private PhotoRankingResponse callClaudeApiForRanking(PhotoRankingRequest request) {
        // Actual API call logic
        // ...
    }
}
```

---

## Using @Retryable Annotation

### Configure @EnableRetry

```java
@Configuration
@EnableRetry
public class RetryConfig {
    // Already configured in RetryConfig.java
}
```

### Basic Annotation Usage

```java
@Service
@Slf4j
public class BioService {

    @Autowired
    private ClaudeApiClient claudeApiClient;

    /**
     * Annotation-based retry
     * - maxAttempts: max number of attempts (including initial call)
     * - backoff: backoff strategy
     *   - delay: initial delay (milliseconds)
     *   - multiplier: delay multiplier
     *   - maxDelay: max delay (milliseconds)
     */
    @Retryable(
        value = {ClaudeApiException.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 1000,
            multiplier = 2,
            maxDelay = 10000
        )
    )
    public BioRewriteResponse rewriteBio(BioRewriteRequest request) {
        log.info("Calling Claude API to rewrite bio");

        // Validate input
        validateBioRequest(request);

        // Call Claude API
        return claudeApiClient.rewriteBio(request);
    }

    /**
     * Recovery method after retries exhausted
     * - Method signature must match the original method
     * - First parameter must be the exception type
     */
    @Recover
    public BioRewriteResponse recoverBioRewrite(
            ClaudeApiException e,
            BioRewriteRequest request) {

        log.error("Failed to rewrite bio after retries. Error: {}", e.getMessage());

        // Choose recovery strategy based on error type
        if (e.getErrorCode() == ErrorCode.CLAUDE_RATE_LIMIT) {
            throw new BusinessException(
                ErrorCode.BIO_GENERATION_FAILED,
                "AI service is temporarily busy, please try again later"
            );
        }

        if (e.getErrorCode() == ErrorCode.CLAUDE_QUOTA_EXCEEDED) {
            throw new BusinessException(
                ErrorCode.BIO_GENERATION_FAILED,
                "AI service quota exceeded, please contact support"
            );
        }

        throw new BusinessException(
            ErrorCode.BIO_GENERATION_FAILED,
            "Failed to generate bio rewrite: " + e.getMessage()
        );
    }

    private void validateBioRequest(BioRewriteRequest request) {
        if (request.getBioText() == null || request.getBioText().length() < 10) {
            throw new BusinessException(ErrorCode.BIO_TOO_SHORT);
        }
        if (request.getBioText().length() > 500) {
            throw new BusinessException(ErrorCode.BIO_TOO_LONG);
        }
    }
}
```

### Conditional Retry

```java
@Service
public class ConversationStarterService {

    /**
     * Only retry specific retryable exceptions
     */
    @Retryable(
        include = {ClaudeApiException.class},
        exclude = {BusinessException.class},
        maxAttempts = 3
    )
    public ConversationStarterResponse generateStarters(ConversationStarterRequest request) {
        // Business exceptions do not trigger retry
        if (request.getMatchProfile() == null) {
            throw new BusinessException(ErrorCode.CONV_INVALID_INPUT);
        }

        // Claude API exceptions trigger retry
        return claudeApiClient.generateConversationStarters(request);
    }
}
```

---

## Manual Retry Logic

### When to Use

When you need finer-grained control, you can implement retry logic manually:

```java
@Service
@Slf4j
public class ManualRetryService {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 1000;
    private static final double MULTIPLIER = 2.0;

    /**
     * Manual exponential backoff retry
     */
    public String callWithManualRetry(String prompt) {
        int attempt = 0;
        long delay = INITIAL_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                log.info("Attempt {} of {}", attempt, MAX_RETRIES);

                return callClaudeApi(prompt);

            } catch (ClaudeApiException e) {
                // If not retryable, throw immediately
                if (!e.isRetryable()) {
                    log.error("Non-retryable error: {}", e.getMessage());
                    throw e;
                }

                // If max retries reached, throw
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries ({}) exceeded", MAX_RETRIES);
                    throw new ClaudeApiException(
                        ErrorCode.RETRY_EXHAUSTED,
                        "Failed after " + MAX_RETRIES + " attempts",
                        e
                    );
                }

                // Calculate delay and wait
                log.warn("Attempt {} failed, retrying after {}ms. Error: {}",
                    attempt, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ClaudeApiException(ErrorCode.SYSTEM_ERROR, ie);
                }

                // Exponentially increase delay
                delay = (long) (delay * MULTIPLIER);
            }
        }

        throw new ClaudeApiException(ErrorCode.RETRY_EXHAUSTED);
    }

    private String callClaudeApi(String prompt) {
        // Actual API call
        // ...
        return "response";
    }
}
```

---

## Testing the Retry Mechanism

### Unit Tests

```java
@SpringBootTest
class RetryMechanismTest {

    @Autowired
    private RetryTemplate claudeApiRetryTemplate;

    @Mock
    private ClaudeApiClient mockApiClient;

    @Test
    void testRetryOnRateLimitError() {
        // Simulate first two calls failing, third succeeding
        when(mockApiClient.callApi(anyString()))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_RATE_LIMIT))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_RATE_LIMIT))
            .thenReturn("success");

        // Execute call with retry
        String result = claudeApiRetryTemplate.execute(context ->
            mockApiClient.callApi("test")
        );

        // Verify result
        assertEquals("success", result);
        // Verify called 3 times
        verify(mockApiClient, times(3)).callApi("test");
    }

    @Test
    void testNoRetryOnNonRetryableError() {
        // Simulate non-retryable error
        when(mockApiClient.callApi(anyString()))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_API_KEY_INVALID));

        // Verify no retry
        assertThrows(ClaudeApiException.class, () ->
            claudeApiRetryTemplate.execute(context ->
                mockApiClient.callApi("test")
            )
        );

        // Verify called only once
        verify(mockApiClient, times(1)).callApi("test");
    }

    @Test
    void testMaxRetriesExceeded() {
        // Simulate persistent failure
        when(mockApiClient.callApi(anyString()))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_TIMEOUT));

        // Verify exception after 3 retries
        assertThrows(ClaudeApiException.class, () ->
            claudeApiRetryTemplate.execute(context ->
                mockApiClient.callApi("test")
            )
        );

        // Verify called 3 times (max retries)
        verify(mockApiClient, times(3)).callApi("test");
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class RetryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaudeApiClient mockApiClient;

    @Test
    void testBioRewriteWithRetry() throws Exception {
        // Simulate first call failing, second succeeding
        when(mockApiClient.rewriteBio(any()))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_TIMEOUT))
            .thenReturn(new BioRewriteResponse("Rewritten bio"));

        // Send request
        mockMvc.perform(post("/api/profile/rewrite-bio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bioText\":\"Original bio\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rewrittenBio").value("Rewritten bio"));

        // Verify retried twice
        verify(mockApiClient, times(2)).rewriteBio(any());
    }
}
```

---

## Best Practices

### 1. Choose the right retry strategy

- **Fail-fast scenarios**: User input errors, invalid API key -> **No retry**
- **Transient failures**: Network timeout, service overloaded -> **Retry 2-3 times**
- **Rate limiting**: Rate limit -> **Retry with exponential backoff**

### 2. Set reasonable timeouts

```java
@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);  // Connection timeout: 5 seconds
    factory.setReadTimeout(30000);    // Read timeout: 30 seconds
    return new RestTemplate(factory);
}
```

### 3. Log retry attempts

```java
retryTemplate.registerListener(new RetryListener() {
    @Override
    public <T, E extends Throwable> void onError(
            RetryContext context,
            RetryCallback<T, E> callback,
            Throwable throwable) {

        log.warn("Retry attempt {}/{} failed: {}",
            context.getRetryCount() + 1,
            maxAttempts,
            throwable.getMessage());
    }
});
```

### 4. Monitor retry metrics

```java
@Component
public class RetryMetrics {

    private final MeterRegistry meterRegistry;

    public void recordRetry(String operation, int attemptNumber) {
        meterRegistry.counter("api.retry",
            "operation", operation,
            "attempt", String.valueOf(attemptNumber)
        ).increment();
    }
}
```

### 5. Prevent cascading failures

```java
// Add circuit breaker
@CircuitBreaker(name = "claudeApi", fallbackMethod = "fallback")
@Retryable(...)
public String callClaudeApi(String prompt) {
    // ...
}

public String fallback(String prompt, Exception e) {
    return "Service temporarily unavailable";
}
```
