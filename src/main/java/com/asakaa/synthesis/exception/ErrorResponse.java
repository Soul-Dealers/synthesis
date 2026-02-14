package com.asakaa.synthesis.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {

    private String code;
    private String message;
    private int httpStatus;
    private LocalDateTime timestamp;

    public static ErrorResponse of(String code, String message, int status) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .httpStatus(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
