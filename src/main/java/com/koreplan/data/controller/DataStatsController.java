package com.koreplan.data.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.service.SearchDataService;
import com.koreplan.data.service.UpdateDataService;
import com.koreplan.data.dto.DataStatsResponse; // ✅ 별도 클래스 import

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/data/stats")
@RequiredArgsConstructor
@Slf4j
public class DataStatsController {
    
    @Autowired
    private UpdateDataService updateDataService;
    
    @Autowired
    private SearchDataService searchDataService;
    
    /**
     * 조회수 증가
     * POST /api/data/stats/{contentId}/view
     */
    @PostMapping("/{contentId}/view")
    public ResponseEntity<String> incrementViewCount(@PathVariable String contentId) {
        try {
            log.info("조회수 증가 요청 - contentId: {}", contentId);
            
            updateDataService.incrementViewCountByContentId(contentId);
            
            log.info("조회수 증가 완료 - contentId: {}", contentId);
            return ResponseEntity.ok("조회수가 증가했습니다.");
            
        } catch (EntityNotFoundException e) {
            log.error("데이터를 찾을 수 없음 - contentId: {}", contentId);
            return ResponseEntity.status(404).body("데이터를 찾을 수 없습니다.");
            
        } catch (Exception e) {
            log.error("조회수 증가 실패 - contentId: {}, error: {}", contentId, e.getMessage());
            return ResponseEntity.status(500).body("조회수 증가에 실패했습니다.");
        }
    }
    
    /**
     * 통계 조회 (DataEntity 기반으로 수정)
     * GET /api/data/stats/{contentId}
     */
    @GetMapping("/{contentId}")
    public ResponseEntity<DataStatsResponse> getSpotStats(@PathVariable String contentId) {
        try {
            log.info("통계 조회 요청 - contentId: {}", contentId);
            
            // ✅ DataEntity에서 바로 통계 정보 조회
            DataEntity dataEntity = searchDataService.getDataWithStatsByContentId(contentId);
            
            DataStatsResponse stats = DataStatsResponse.builder()
                .contentId(dataEntity.getContentId())
                .viewCount(dataEntity.getViewCount())
                .rating(dataEntity.getRating())
                .reviewCount(dataEntity.getReviewCount())
                .score(dataEntity.getScore())  // ✅ 종합 점수 추가
                .build();
            
            log.info("통계 조회 완료 - contentId: {}, viewCount: {}, score: {}", 
                contentId, stats.getViewCount(), stats.getScore());
            return ResponseEntity.ok(stats);
            
        } catch (EntityNotFoundException e) {
            log.error("데이터를 찾을 수 없음 - contentId: {}", contentId);
            return ResponseEntity.status(404).body(null);
            
        } catch (Exception e) {
            log.error("통계 조회 실패 - contentId: {}, error: {}", contentId, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }
}