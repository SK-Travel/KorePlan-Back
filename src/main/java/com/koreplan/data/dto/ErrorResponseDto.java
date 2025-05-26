package com.koreplan.data.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    
    public static ErrorResponseDto of(String error, String message, String path) {
        return ErrorResponseDto.builder()
            .error(error)
            .message(message)
            .path(path)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
