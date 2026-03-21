# Error Handling Quick Start

## Created Files

### Core Classes
1. **ErrorCode.java** - Error code enum (all custom error codes)
2. **ClaudeApiException.java** - Claude API specific exception
3. **BusinessException.java** - Business exception base class
4. **RetryConfig.java** - Retry configuration (exponential backoff)
5. **ErrorResponse.java** (updated) - Error response DTO
6. **GlobalExceptionHandler.java** (updated) - Global exception handler

### Documentation
1. **ERROR_CODES.md** - Complete error code reference
2. **RETRY_USAGE_EXAMPLE.md** - Retry mechanism usage examples
3. **ERROR_HANDLING_QUICKSTART.md** - This file

### Dependency Updates
- **pom.xml** - Added Spring Retry and AOP dependencies

---

## Quick Usage

### 1. Throw a business exception

```java
// Simple usage
throw new BusinessException(ErrorCode.BIO_TOO_SHORT);

// Custom message
throw new BusinessException(
    ErrorCode.PHOTO_TOO_MANY,
    "You provided " + photos.size() + " photos"
);
```

### 2. Throw a Claude API exception

```java
// Auto-create from HTTP status code
throw ClaudeApiException.fromHttpStatus(429, "Rate limit exceeded");

// Use error code directly
throw new ClaudeApiException(ErrorCode.CLAUDE_TIMEOUT);
```

### 3. Use retry mechanism (recommended)

```java
@Service
@RequiredArgsConstructor
public class MyService {

    private final RetryTemplate claudeApiRetryTemplate;

    public String callApi(String input) {
        return claudeApiRetryTemplate.execute(context -> {
            // Your Claude API call logic
            // Retryable exceptions are automatically retried
            return makeApiCall(input);
        });
    }
}
```

### 4. Use annotation-based retry

```java
@Service
public class MyService {

    @Retryable(
        value = ClaudeApiException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String callApi(String input) {
        // Automatically retries on ClaudeApiException
        return makeApiCall(input);
    }

    @Recover
    public String recover(ClaudeApiException e, String input) {
        // Fallback logic after retries exhausted
        log.error("Failed after retries", e);
        throw new BusinessException(ErrorCode.RETRY_EXHAUSTED);
    }
}
```

---

## Error Response Format

Error response received by the client:

```json
{
  "code": 4002,
  "status": 429,
  "error": "CLAUDE_RATE_LIMIT",
  "message": "Claude API rate limit exceeded",
  "path": "/api/profile/rewrite-bio",
  "timestamp": "2026-03-20T10:30:00",
  "retryable": true
}
```

---

## Common Error Codes

| Code | Name | Description | Retryable |
|------|------|-------------|-----------|
| 1000 | BAD_REQUEST | Invalid request parameters | No |
| 1100 | BIO_TOO_SHORT | Bio too short | No |
| 1101 | BIO_TOO_LONG | Bio too long | No |
| 2001 | PHOTO_TOO_FEW | Too few photos | No |
| 2002 | PHOTO_TOO_MANY | Too many photos | No |
| 4001 | CLAUDE_API_KEY_INVALID | API key invalid | No |
| 4002 | CLAUDE_RATE_LIMIT | Rate limited | Yes |
| 4005 | CLAUDE_TIMEOUT | Request timeout | Yes |
| 4006 | CLAUDE_OVERLOADED | Service overloaded | Yes |
| 9000 | SYSTEM_ERROR | System error | No |

See [ERROR_CODES.md](ERROR_CODES.md) for the full list.

---

## Retry Configuration

### Claude API Retry Policy

- **Max retry attempts**: 3
- **Initial delay**: 1 second
- **Delay multiplier**: 2 (exponential backoff)
- **Max delay**: 10 seconds

**Retry schedule:**
- 1st failure -> retry after 1 second
- 2nd failure -> retry after 2 seconds
- 3rd failure -> retry after 4 seconds

### Retryable Errors

Only the following errors are automatically retried:
- `CLAUDE_RATE_LIMIT` (429)
- `CLAUDE_TIMEOUT` (408)
- `CLAUDE_OVERLOADED` (503)
- `CLAUDE_SERVER_ERROR` (500/502/504)
- `CLAUDE_CONNECTION_ERROR` (connection failure)

---

## Service Layer Example

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BioService {

    private final RetryTemplate claudeApiRetryTemplate;
    private final RestTemplate restTemplate;

    @Value("${claude.api.key}")
    private String apiKey;

    public BioRewriteResponse rewriteBio(BioRewriteRequest request) {
        // 1. Validate parameters - throws BusinessException
        validateBioRequest(request);

        // 2. API call with retry - automatically handles ClaudeApiException
        return claudeApiRetryTemplate.execute(context -> {
            log.info("Calling Claude API (attempt {})", context.getRetryCount() + 1);
            return callClaudeApi(request);
        });
    }

    private void validateBioRequest(BioRewriteRequest request) {
        if (request.getBioText() == null || request.getBioText().length() < 10) {
            throw new BusinessException(ErrorCode.BIO_TOO_SHORT);
        }
        if (request.getBioText().length() > 500) {
            throw new BusinessException(ErrorCode.BIO_TOO_LONG);
        }
    }

    private BioRewriteResponse callClaudeApi(BioRewriteRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Build request
            Map<String, Object> body = buildApiRequest(request);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // Call API
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages",
                entity,
                Map.class
            );

            // Parse response
            return parseResponse(response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // HTTP error -> ClaudeApiException (may trigger retry)
            throw ClaudeApiException.fromHttpStatus(
                e.getStatusCode().value(),
                e.getMessage()
            );
        } catch (ResourceAccessException e) {
            // Connection error -> ClaudeApiException (triggers retry)
            throw new ClaudeApiException(
                ErrorCode.CLAUDE_CONNECTION_ERROR,
                "Failed to connect to Claude API",
                e
            );
        }
    }

    private Map<String, Object> buildApiRequest(BioRewriteRequest request) {
        return Map.of(
            "model", "claude-sonnet-4-6",
            "messages", List.of(Map.of(
                "role", "user",
                "content", "Rewrite this bio: " + request.getBioText()
            )),
            "max_tokens", 500
        );
    }

    private BioRewriteResponse parseResponse(Map<String, Object> body) {
        try {
            List<Map<String, Object>> content =
                (List<Map<String, Object>>) body.get("content");
            String text = (String) content.get(0).get("text");
            return new BioRewriteResponse(text);
        } catch (Exception e) {
            throw new ClaudeApiException(
                ErrorCode.CLAUDE_RESPONSE_PARSE_ERROR,
                "Failed to parse API response",
                e
            );
        }
    }
}
```

---

## Testing

### Unit Test Example

```java
@SpringBootTest
class BioServiceTest {

    @Autowired
    private BioService bioService;

    @Test
    void testBioTooShort() {
        BioRewriteRequest request = new BioRewriteRequest("short");

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> bioService.rewriteBio(request)
        );

        assertEquals(ErrorCode.BIO_TOO_SHORT, exception.getErrorCode());
    }

    @Test
    void testRetryOnTimeout() {
        // Test retry mechanism
        // ...
    }
}
```

---

## More Information

- **Full error code list**: [ERROR_CODES.md](ERROR_CODES.md)
- **Detailed retry examples**: [RETRY_USAGE_EXAMPLE.md](RETRY_USAGE_EXAMPLE.md)

---

## Next Steps

1. **Run `mvn clean install`** to install new dependencies
2. **Use error codes in Services** to replace generic exception throwing
3. **Test the retry mechanism** by simulating Claude API failures
4. **Configure monitoring and alerting** for critical error codes

---

## Best Practices

### DO
- Use specific error codes instead of generic exceptions
- Use RetryTemplate for retryable errors
- Log error codes and context information
- Document the handling approach for each error

### DON'T
- Don't catch exceptions without handling them
- Don't retry non-retryable errors
- Don't include sensitive information in business exceptions
- Don't use magic numbers; always use ErrorCode enum
