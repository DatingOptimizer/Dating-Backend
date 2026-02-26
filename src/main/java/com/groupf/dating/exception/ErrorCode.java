package com.groupf.dating.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom error code enumeration
 * Error code format: [Module Prefix][Error Type][Serial Number]
 * - Module Prefix: BIO(1xxx), PHOTO(2xxx), CONV(3xxx), CLAUDE(4xxx), SYS(9xxx)
 * - Error Type: Business errors(0-4), External service errors(5-7), System errors(8-9)
 */
@Getter
public enum ErrorCode {

    // ============ General Errors (1000-1099) ============
    SUCCESS(0, "Success", HttpStatus.OK),
    BAD_REQUEST(1000, "Invalid request parameters", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1001, "Unauthorized access", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1002, "Access forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND(1003, "Resource not found", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(1004, "Validation failed", HttpStatus.BAD_REQUEST),

    // ============ Bio Related Errors (1100-1199) ============
    BIO_TOO_SHORT(1100, "Bio text is too short (minimum 10 characters)", HttpStatus.BAD_REQUEST),
    BIO_TOO_LONG(1101, "Bio text is too long (maximum 500 characters)", HttpStatus.BAD_REQUEST),
    BIO_INVALID_FORMAT(1102, "Bio contains invalid characters or format", HttpStatus.BAD_REQUEST),
    BIO_GENERATION_FAILED(1150, "Failed to generate bio rewrite", HttpStatus.INTERNAL_SERVER_ERROR),

    // ============ Photo Related Errors (2000-2099) ============
    PHOTO_INVALID_URL(2000, "Invalid photo URL format", HttpStatus.BAD_REQUEST),
    PHOTO_INVALID_FORMAT(2003, "Invalid image format, only JPEG and PNG are supported", HttpStatus.BAD_REQUEST),
    PHOTO_TOO_FEW(2001, "At least 2 photos required for ranking", HttpStatus.BAD_REQUEST),
    PHOTO_TOO_MANY(2002, "Maximum 5 photos allowed", HttpStatus.BAD_REQUEST),
    PHOTO_TOO_LARGE(2004, "Image size must not exceed 10MB", HttpStatus.BAD_REQUEST),
    PHOTO_DOWNLOAD_FAILED(2050, "Failed to download photo from URL", HttpStatus.BAD_REQUEST),
    PHOTO_PROCESS_FAILED(2052, "Failed to process photo", HttpStatus.INTERNAL_SERVER_ERROR),
    PHOTO_ANALYSIS_FAILED(2051, "Failed to analyze photos", HttpStatus.INTERNAL_SERVER_ERROR),

    // ============ Conversation Related Errors (3000-3099) ============
    CONV_INVALID_INPUT(3000, "Invalid conversation input", HttpStatus.BAD_REQUEST),
    CONV_CONTEXT_TOO_LONG(3001, "Context information is too long", HttpStatus.BAD_REQUEST),
    CONV_GENERATION_FAILED(3050, "Failed to generate conversation starters", HttpStatus.INTERNAL_SERVER_ERROR),

    // ============ Claude API Related Errors (4000-4099) ============
    CLAUDE_API_KEY_MISSING(4000, "Claude API key is not configured", HttpStatus.INTERNAL_SERVER_ERROR),
    CLAUDE_API_KEY_INVALID(4001, "Claude API key is invalid", HttpStatus.UNAUTHORIZED),
    CLAUDE_RATE_LIMIT(4002, "Claude API rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    CLAUDE_QUOTA_EXCEEDED(4003, "Claude API quota exceeded", HttpStatus.PAYMENT_REQUIRED),
    CLAUDE_INVALID_REQUEST(4004, "Invalid request to Claude API", HttpStatus.BAD_REQUEST),
    CLAUDE_TIMEOUT(4005, "Claude API request timeout", HttpStatus.REQUEST_TIMEOUT),
    CLAUDE_OVERLOADED(4006, "Claude API is overloaded", HttpStatus.SERVICE_UNAVAILABLE),
    CLAUDE_SERVER_ERROR(4007, "Claude API server error", HttpStatus.INTERNAL_SERVER_ERROR),
    CLAUDE_CONNECTION_ERROR(4008, "Failed to connect to Claude API", HttpStatus.SERVICE_UNAVAILABLE),
    CLAUDE_RESPONSE_PARSE_ERROR(4009, "Failed to parse Claude API response", HttpStatus.INTERNAL_SERVER_ERROR),

    // ============ System Errors (9000-9099) ============
    SYSTEM_ERROR(9000, "Internal system error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(9001, "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_UPLOAD_SIZE_EXCEEDED(9002, "File upload size exceeded", HttpStatus.PAYLOAD_TOO_LARGE),
    EXTERNAL_SERVICE_ERROR(9003, "External service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    RETRY_EXHAUSTED(9004, "Maximum retry attempts exhausted", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * Find error code enum by code number
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }

    /**
     * Check if the error is retryable
     */
    public boolean isRetryable() {
        return this == CLAUDE_TIMEOUT
            || this == CLAUDE_OVERLOADED
            || this == CLAUDE_RATE_LIMIT
            || this == CLAUDE_CONNECTION_ERROR
            || this == EXTERNAL_SERVICE_ERROR;
    }
}
