package com.koreplan.controller.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.image.ErrorResponse;
import com.koreplan.dto.image.ImageApiResponseDto;
import com.koreplan.dto.image.NoImagesResponse;
import com.koreplan.service.image.ImagesLoadService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/spots")
@CrossOrigin(origins = "*", allowCredentials = "false")
@Slf4j
public class ImageController {
    
    @Autowired
    private ImagesLoadService imagesLoadService;
    
    /**
     * contentId로 이미지 정보 조회
     * GET /api/spots/{contentId}/images
     */
    @GetMapping("/{contentId}/images")
    public ResponseEntity<?> getSpotImages(@PathVariable String contentId) {
        
        log.info("이미지 조회 요청: contentId={}", contentId);
        
        if (contentId == null || contentId.trim().isEmpty()) {
            log.warn("contentId가 비어있음");
            return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_CONTENT_ID", "contentId가 필요합니다"));
        }
        
        try {
            ImageApiResponseDto images = imagesLoadService.getImages(contentId);
            
            // 이미지 개수 확인
            int totalCount = images.getResponse().getBody().getTotalCount();
            if (totalCount == 0) {
                log.info("이미지 없음: contentId={}", contentId);
                return ResponseEntity.ok(new NoImagesResponse("NO_IMAGES", "등록된 이미지가 없습니다", contentId));
            }
            
            return ResponseEntity.ok(images);
            
        } catch (IllegalArgumentException e) {
            // 유효성 검사 실패
            log.warn("유효성 검사 실패: contentId={}, error={}", contentId, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
            
        } catch (RuntimeException e) {
            // API 호출 실패 또는 파싱 실패
            log.error("이미지 로드 실패: contentId={}, error={}", contentId, e.getMessage());
            return ResponseEntity.status(502).body(new ErrorResponse("API_ERROR", "외부 API 호출에 실패했습니다"));
            
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            log.error("예상치 못한 오류: contentId={}", contentId, e);
            return ResponseEntity.status(500).body(new ErrorResponse("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다"));
        }
    }
    
    @GetMapping("/test")
    public String test() {
        return "API 연결 성공!";
    }
}