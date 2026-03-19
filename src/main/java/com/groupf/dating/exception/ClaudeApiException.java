package com.groupf.dating.exception;

import lombok.Getter;

/**
 * Claude API related exception
 */
@Getter
public class ClaudeApiException extends BusinessException {

    private final int httpStatusCode;
    private final boolean retryable;

    public ClaudeApiException(ErrorCode errorCode) {
        super(errorCode);
        this.httpStatusCode = errorCode.getHttpStatus().value();
        this.retryable = errorCode.isRetryable();
    }

    public ClaudeApiException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
        this.httpStatusCode = errorCode.getHttpStatus().value();
        this.retryable = errorCode.isRetryable();
    }

    public ClaudeApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.httpStatusCode = errorCode.getHttpStatus().value();
        this.retryable = errorCode.isRetryable();
    }

    public ClaudeApiException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
        this.httpStatusCode = errorCode.getHttpStatus().value();
        this.retryable = errorCode.isRetryable();
    }

    /**
     * Create exception based on HTTP status code
     */
    public static ClaudeApiException fromHttpStatus(int statusCode, String message) {
        ErrorCode errorCode = switch (statusCode) {
            case 401 -> ErrorCode.CLAUDE_API_KEY_INVALID;
            case 408 -> ErrorCode.CLAUDE_TIMEOUT;
            case 429 -> ErrorCode.CLAUDE_RATE_LIMIT;
            case 402 -> ErrorCode.CLAUDE_QUOTA_EXCEEDED;
            case 400 -> ErrorCode.CLAUDE_INVALID_REQUEST;
            case 503 -> ErrorCode.CLAUDE_OVERLOADED;
            case 500, 502, 504 -> ErrorCode.CLAUDE_SERVER_ERROR;
            default -> ErrorCode.CLAUDE_CONNECTION_ERROR;
        };

        return new ClaudeApiException(errorCode, message);
    }
}
