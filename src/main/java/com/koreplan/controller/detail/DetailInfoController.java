package com.koreplan.controller.detail;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.service.detail.DetailInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/detail")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowCredentials = "false")
public class DetailInfoController {
    
    private final DetailInfoService detailInfoService;
    
    /**
     * 상세정보 조회 API
     */
    @GetMapping("/{contentId}/intro")
    public ResponseEntity<?> getDetailIntro(
            @PathVariable String contentId,
            @RequestParam String contentTypeId) {
        
        log.info("상세정보 조회 요청 - contentId: {}, contentTypeId: {}", contentId, contentTypeId);
        
        if (contentId == null || contentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("contentId가 필요합니다.");
        }
        
        if (contentTypeId == null || contentTypeId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("contentTypeId가 필요합니다.");
        }
        
        try {
            ResponseEntity<Object> detailInfo = detailInfoService.getDetailIntro(contentId, contentTypeId);
            return detailInfo;
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 contentTypeId: {}", contentTypeId);
            return ResponseEntity.badRequest().body("지원하지 않는 contentTypeId입니다: " + contentTypeId);
            
        } catch (Exception e) {
            log.error("상세정보 조회 중 오류 발생", e);
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 테스트용 API
     */
    @GetMapping("/test")
    public String test() {
        return "Detail Intro API 연결 성공!";
    }
}
