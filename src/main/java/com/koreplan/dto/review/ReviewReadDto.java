package com.koreplan.dto.review;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ReviewReadDto {
    private Long reviewId;
    private String content;
    private int rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User 정보
    private Integer userId;
    private String name;
    
    // Data 정보  
    private Long dataId;
    private String contentId;
    private String dataTitle;
    
    
}
