package com.groupf.dating.exception;

import com.groupf.dating.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Unwrap Spring Retry's ExhaustedRetryException and re-handle the real cause
     */
    @ExceptionHandler(ExhaustedRetryException.class)
    public ResponseEntity<ErrorResponse> handleExhaustedRetryException(
            ExhaustedRetryException ex, WebRequest request) {
        Throwable cause = ex.getCause();
        if (cause instanceof ClaudeApiException claudeEx) {
            return handleClaudeApiException(claudeEx, request);
        }
        if (cause instanceof BusinessException bEx) {
            return handleBusinessException(bEx, request);
        }
        if (cause instanceof RuntimeException rEx) {
            return handleRuntimeException(rEx, request);
        }
        return handleGeneralException(ex, request);
    }

    /**
     * Handle Claude API exceptions
     */
    @ExceptionHandler(ClaudeApiException.class)
    public ResponseEntity<ErrorResponse> handleClaudeApiException(
            ClaudeApiException ex, WebRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        log.error("Claude API error [{}]: {}", errorCode.getCode(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .code(errorCode.getCode())
                .status(errorCode.getHttpStatus().value())
                .error(errorCode.name())
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .retryable(ex.isRetryable())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }

    /**
     * Handle business exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Business error [{}]: {}", errorCode.getCode(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code(errorCode.getCode())
                .status(errorCode.getHttpStatus().value())
                .error(errorCode.name())
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.BAD_REQUEST.getCode())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        log.warn("Validation error: {}", message);

        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeException(
            MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("File upload size exceeded");

        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.FILE_UPLOAD_SIZE_EXCEEDED.getCode())
                .status(HttpStatus.CONTENT_TOO_LARGE.value())
                .error("File Too Large")
                .message("Maximum upload size exceeded")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        log.error("Runtime error", ex);

        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.SYSTEM_ERROR.getCode())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);

        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.SYSTEM_ERROR.getCode())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
