package com.koreplan.controller.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.image.ImageApiResponseDto;
import com.koreplan.service.image.ImagesLoadService;

@RestController
@RequestMapping("/api/spots")
@CrossOrigin(origins = "*", allowCredentials = "false")  // allowCredentials를 false로 명시
public class ImageController {
    
    @Autowired
    private ImagesLoadService imagesLoadService;
    
    /**
     * contentId로 이미지 정보 조회
     * GET /api/spots/{contentId}/images
     */
    @GetMapping("/{contentId}/images")
    public ResponseEntity<?> getSpotImages(@PathVariable String contentId) {
        
        if (contentId == null || contentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("contentId가 필요합니다.");
        }
        
        try {
            ImageApiResponseDto images = imagesLoadService.getImages(contentId);
            return ResponseEntity.ok(images);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    @GetMapping("/test")
    public String test() {
        return "API 연결 성공!";
    }
}
