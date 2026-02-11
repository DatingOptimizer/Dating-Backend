package com.groupf.dating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private int code;           // Custom error code
    private int status;         // HTTP status code
    private String error;       // Error type
    private String message;     // Error message
    private String path;        // Request path
    private LocalDateTime timestamp;
    private Boolean retryable;  // Whether the error is retryable (optional)
}
