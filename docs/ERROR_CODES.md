# Error Code Reference

## Error Code Format

Error codes use a categorized numbering scheme: `[module prefix][error type][sequence]`

- **Module prefix**: BIO(1xxx), PHOTO(2xxx), CONV(3xxx), CLAUDE(4xxx), SYS(9xxx)
- **Error type**: Business errors(0-4), External service errors(5-7), System errors(8-9)

## Error Response Format

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

## Error Code List

### General Errors (1000-1099)

| Code | Name | HTTP Status | Description | Retryable |
|------|------|-------------|-------------|-----------|
| 0 | SUCCESS | 200 | Request successful | - |
| 1000 | BAD_REQUEST | 400 | Invalid request parameters | No |
| 1001 | UNAUTHORIZED | 401 | Unauthorized access | No |
| 1002 | FORBIDDEN | 403 | Access forbidden | No |
| 1003 | NOT_FOUND | 404 | Resource not found | No |
| 1004 | VALIDATION_ERROR | 400 | Parameter validation failed | No |

### Bio Errors (1100-1199)

| Code | Name | HTTP Status | Description | Retryable |
|------|------|-------------|-------------|-----------|
| 1100 | BIO_TOO_SHORT | 400 | Bio text too short (min 10 chars) | No |
| 1101 | BIO_TOO_LONG | 400 | Bio text too long (max 500 chars) | No |
| 1102 | BIO_INVALID_FORMAT | 400 | Bio contains invalid characters or format | No |
| 1150 | BIO_GENERATION_FAILED | 500 | Bio rewrite generation failed | Yes |

### Photo Errors (2000-2099)

| Code | Name | HTTP Status | Description | Retryable |
|------|------|-------------|-------------|-----------|
| 2000 | PHOTO_INVALID_URL | 400 | Invalid photo URL format | No |
| 2001 | PHOTO_TOO_FEW | 400 | Not enough photos (min 2) | No |
| 2002 | PHOTO_TOO_MANY | 400 | Too many photos (max 10) | No |
| 2050 | PHOTO_DOWNLOAD_FAILED | 400 | Photo download failed | Yes |
| 2051 | PHOTO_ANALYSIS_FAILED | 500 | Photo analysis failed | Yes |

### Conversation Errors (3000-3099)

| Code | Name | HTTP Status | Description | Retryable |
|------|------|-------------|-------------|-----------|
| 3000 | CONV_INVALID_INPUT | 400 | Invalid conversation input | No |
| 3001 | CONV_CONTEXT_TOO_LONG | 400 | Context too long | No |
| 3050 | CONV_GENERATION_FAILED | 500 | Conversation starter generation failed | Yes |

### Claude API Errors (4000-4099)

| Code | Name | HTTP Status | Description | Retryable |
|------|------|-------------|-------------|-----------|
| 4000 | CLAUDE_API_KEY_MISSING | 500 | Claude API key not configured | No |
| 4001 | CLAUDE_API_KEY_INVALID | 401 | Claude API key invalid | No |
| 4002 | CLAUDE_RATE_LIMIT | 429 | Claude API rate limit exceeded | Yes |
| 4003 | CLAUDE_QUOTA_EXCEEDED | 402 | Claude API quota exceeded | No |
| 4004 | CLAUDE_INVALID_REQUEST | 400 | Claude API invalid request | No |
| 4005 | CLAUDE_TIMEOUT | 408 | Claude API request timeout | Yes |
| 4006 | CLAUDE_OVERLOADED | 503 | Claude API service overloaded | Yes |
| 4007 | CLAUDE_SERVER_ERROR | 500 | Claude API server error | Yes |
| 4008 | CLAUDE_CONNECTION_ERROR | 503 | Claude API connection failed | Yes |
| 4009 | CLAUDE_RESPONSE_PARSE_ERROR | 500 | Claude API response parse failed | No |

### System Errors (9000-9099)

| Code | Name | HTTP Status | Description | Retryable |
|------|------|-------------|-------------|-----------|
| 9000 | SYSTEM_ERROR | 500 | Internal system error | No |
| 9001 | DATABASE_ERROR | 500 | Database operation failed | No |
| 9002 | FILE_UPLOAD_SIZE_EXCEEDED | 413 | File upload size exceeded | No |
| 9003 | EXTERNAL_SERVICE_ERROR | 503 | External service unavailable | Yes |
| 9004 | RETRY_EXHAUSTED | 503 | Retry attempts exhausted | No |

## Retry Mechanism

### Claude API Retry Policy

**Auto-retried error codes:**
- `4002` CLAUDE_RATE_LIMIT (rate limited)
- `4005` CLAUDE_TIMEOUT (request timeout)
- `4006` CLAUDE_OVERLOADED (service overloaded)
- `4007` CLAUDE_SERVER_ERROR (server error)
- `4008` CLAUDE_CONNECTION_ERROR (connection error)

**Retry configuration:**
- Max retry attempts: 3
- Initial delay: 1 second
- Delay multiplier: 2 (exponential backoff)
- Max delay: 10 seconds

**Retry schedule:**
- 1st retry: after 1 second
- 2nd retry: after 2 seconds
- 3rd retry: after 4 seconds

### Usage Examples

#### 1. Throw a business exception

```java
// Validation failure
if (bioText.length() < 10) {
    throw new BusinessException(ErrorCode.BIO_TOO_SHORT);
}

// Custom error message
if (photos.size() > 10) {
    throw new BusinessException(
        ErrorCode.PHOTO_TOO_MANY,
        "You provided " + photos.size() + " photos, but maximum is 10"
    );
}
```

#### 2. Throw a Claude API exception

```java
// Create exception from HTTP status code
catch (HttpClientErrorException e) {
    throw ClaudeApiException.fromHttpStatus(
        e.getStatusCode().value(),
        "Failed to call Claude API: " + e.getMessage()
    );
}

// Throw a specific error directly
if (apiKey == null || apiKey.isEmpty()) {
    throw new ClaudeApiException(ErrorCode.CLAUDE_API_KEY_MISSING);
}
```

#### 3. Use RetryTemplate

```java
@Service
public class ClaudeApiService {

    @Autowired
    private RetryTemplate claudeApiRetryTemplate;

    public String callClaudeApi(String prompt) {
        return claudeApiRetryTemplate.execute(context -> {
            // Call Claude API
            // Retryable ClaudeApiExceptions are automatically retried
            return makeApiCall(prompt);
        });
    }
}
```

#### 4. Use annotation-based retry

```java
@Service
public class BioService {

    @Retryable(
        value = ClaudeApiException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public BioRewriteResponse rewriteBio(BioRewriteRequest request) {
        // Automatically retries on ClaudeApiException
        return claudeApiClient.rewriteBio(request);
    }

    @Recover
    public BioRewriteResponse recover(ClaudeApiException e, BioRewriteRequest request) {
        // Fallback after retries exhausted
        log.error("Failed to rewrite bio after retries", e);
        throw new BusinessException(ErrorCode.RETRY_EXHAUSTED,
            "Failed to generate bio rewrite after multiple attempts");
    }
}
```

## Frontend Handling Recommendations

### 1. Decide whether to retry based on the retryable field

```javascript
async function callApi() {
  try {
    const response = await fetch('/api/profile/rewrite-bio', {
      method: 'POST',
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      const error = await response.json();

      // Show retry button if error is retryable
      if (error.retryable) {
        showRetryButton();
      } else {
        showErrorMessage(error.message);
      }
    }
  } catch (error) {
    // Network error, show retry button
    showRetryButton();
  }
}
```

### 2. Display user-friendly messages based on error code

```javascript
const ERROR_MESSAGES = {
  1100: 'Your bio is too short. Please enter at least 10 characters.',
  1101: 'Your bio is too long. Please keep it under 500 characters.',
  2001: 'Please upload at least 2 photos for ranking.',
  4002: 'AI service is busy. Please try again later.',
  4003: 'AI service quota exceeded. Please contact the administrator.'
};

function showUserFriendlyError(errorCode) {
  const message = ERROR_MESSAGES[errorCode] || 'Operation failed. Please try again.';
  alert(message);
}
```

## Monitoring and Alerting

### Error codes to monitor

**High priority (immediate alert):**
- `4000` CLAUDE_API_KEY_MISSING
- `4001` CLAUDE_API_KEY_INVALID
- `4003` CLAUDE_QUOTA_EXCEEDED
- `9000` SYSTEM_ERROR
- `9001` DATABASE_ERROR

**Medium priority (frequency-based alert):**
- `4002` CLAUDE_RATE_LIMIT (> 10 per hour)
- `4005` CLAUDE_TIMEOUT (> 20 per hour)
- `4006` CLAUDE_OVERLOADED (> 10 per hour)

**Low priority (log only):**
- Business validation errors (1xxx, 2xxx, 3xxx)
- Client parameter errors (1000-1004)

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-03-15 | Initial version, defined all error codes |
