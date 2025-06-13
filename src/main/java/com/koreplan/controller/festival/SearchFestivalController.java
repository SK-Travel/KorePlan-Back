package com.koreplan.controller.festival;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.festival.FestivalResponseDto;
import com.koreplan.entity.festival.FestivalEntity;
import com.koreplan.service.festival.SearchFestivalService;
import com.koreplan.service.festival.UpdateFestivalService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/festival")
@RequiredArgsConstructor
public class SearchFestivalController {
    
    private final SearchFestivalService searchFestivalService;
    private final UpdateFestivalService updateFestivalService;
    /**
     * 통합 축제 조회 API - 모든 필터링 조합 지원
     * GET /api/festival/search?region={지역}&category={카테고리}&status={상태}&month={월}
     * 
     * 예시:
     * - 전체: /api/festival/search
     * - 지역별: /api/festival/search?region=서울특별시
     * - 카테고리별: /api/festival/search?category=축제
     * - 진행중: /api/festival/search?status=진행중
     * - 진행예정: /api/festival/search?status=진행예정
     * - 특정월: /api/festival/search?month=6
     * - 조합: /api/festival/search?region=서울특별시&category=축제&status=진행중
     */
    @GetMapping("/search")
    @Operation(summary = "통합 축제 조회 API", description = "축제 페이지에서 모든 경우의 수에 대한 조회")
    public ResponseEntity<List<FestivalResponseDto>> searchFestivals(
            @RequestParam(required = false, defaultValue = "") String region,
            @RequestParam(required = false, defaultValue = "") String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer month) {
        try {
            log.info("🔍 통합 검색 요청: region={}, category={}, status={}, month={}", 
                region, category, status, month);
            
            // 월 유효성 검사
            if (month != null && (month < 1 || month > 12)) {
                log.warn("잘못된 월 파라미터: {}", month);
                return ResponseEntity.badRequest().build();
            }
            
            // 빈 문자열을 null로 변환
            String finalRegion = (region != null && !region.trim().isEmpty() && !"전국".equals(region.trim())) ? region.trim() : null;
            String finalCategory = (category != null && !category.trim().isEmpty() && !"전체".equals(category.trim())) ? category.trim() : null;
            String finalStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
            
            log.info("🔍 정제된 파라미터: region={}, category={}, status={}, month={}", 
                finalRegion, finalCategory, finalStatus, month);
            
            List<FestivalEntity> festivals = searchFestivalService.getComplexFilteredFestivals(
                finalRegion, finalCategory, finalStatus, month);
                
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            
            log.info("🎪 검색 결과: {}개 축제 조회됨", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("축제 검색 실패: region={}, category={}, status={}, month={}", 
                region, category, status, month, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 키워드 검색 (프론트엔드에서 실시간 검색에 사용)
     * GET /api/festival/keyword?q={검색어}
     */
    @GetMapping("/keyword")
    @Operation(summary = "축제 이름 검색", description = "축제 이름으로 조회합니다.")
    public ResponseEntity<List<FestivalResponseDto>> searchByKeyword(
            @RequestParam String q) {
        try {
            if (q == null || q.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            log.info("🔎 키워드 검색 요청: q={}", q);
            List<FestivalEntity> festivals = searchFestivalService.searchFestivalsByKeyword(q);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            
            log.info("🔎 키워드 검색 결과: {}개 축제 조회됨", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("키워드 검색 실패: {}", q, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 인기 축제 조회 (메인페이지용 - 기본 5개)
     * GET /api/festival/popular
     */
    @GetMapping("/popular")
    @Operation(summary = "축제 Top5 조회 (메인페이지용)", description = "조회수 Top5 축제/공연/행사 조회")
    public ResponseEntity<List<FestivalResponseDto>> getPopularFestivals() {
        try {
            log.info("🏆 인기 축제 조회 요청 (메인페이지용)");
            List<FestivalEntity> festivals = searchFestivalService.getPopularFestivals();
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            log.info("🏆 인기 축제 조회 완료 (메인페이지용): {}개", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("인기 축제 조회 실패 (메인페이지용)", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/{contentId}/view")
    public ResponseEntity<String> increaseViewCount(@PathVariable String contentId) {
        try {
            log.info("축제 조회수 증가 API 호출 - contentId: {}", contentId);
            
            updateFestivalService.increaseViewCount(contentId);
            
            return ResponseEntity.ok("조회수가 증가되었습니다.");
            
        } catch (Exception e) {
            log.error("조회수 증가 실패 - contentId: {}", contentId, e);
            return ResponseEntity.badRequest().body("조회수 증가에 실패했습니다: " + e.getMessage());
        }
    }

    
}