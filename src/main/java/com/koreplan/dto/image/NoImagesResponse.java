package com.koreplan.dto.image;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoImagesResponse {
    private String code;
    private String message;
    private String contentId;
    private int imageCount = 0;
    
    // 특정 필드만 받는 생성자 (imageCount는 기본값 0)
    public NoImagesResponse(String code, String message, String contentId) {
        this.code = code;
        this.message = message;
        this.contentId = contentId;
        this.imageCount = 0;
    }
}
