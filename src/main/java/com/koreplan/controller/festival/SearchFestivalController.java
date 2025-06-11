package com.koreplan.controller.festival;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.festival.FestivalResponseDto;
import com.koreplan.entity.festival.FestivalEntity;
import com.koreplan.service.festival.SearchFestivalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/festival")
@RequiredArgsConstructor
public class SearchFestivalController {
    
    private final SearchFestivalService searchFestivalService;

    /**
     * 1. 전체 축제 조회
     * GET /api/festival/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<FestivalResponseDto>> getAllFestivals() {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getFestivalByTwoOpt("", "");
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("전체 축제 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 2. 카테고리별 축제 조회 (축제/공연/행사)
     * GET /api/festival/category/{categoryName}
     */
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<FestivalResponseDto>> getFestivalsByCategory(
            @PathVariable String categoryName) {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getFestivalByC2Code(categoryName);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리별 축제 조회 실패: {}", categoryName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 3. 지역별 축제 조회
     * GET /api/festival/region/{regionName}
     */
    @GetMapping("/region/{regionName}")
    public ResponseEntity<List<FestivalResponseDto>> getFestivalsByRegion(
            @PathVariable String regionName) {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getFestivalByRegion(regionName);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("지역별 축제 조회 실패: {}", regionName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 4. 이중 필터링 (지역 + 카테고리)
     * GET /api/festival/filter?region={regionName}&category={categoryName}
     */
    @GetMapping("/filter")
    public ResponseEntity<List<FestivalResponseDto>> getFestivalsByFilter(
            @RequestParam(required = false, defaultValue = "") String region,
            @RequestParam(required = false, defaultValue = "") String category) {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getFestivalByTwoOpt(region, category);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("이중 필터링 조회 실패: region={}, category={}", region, category, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 5. 현재 진행중인 축제 조회
     * GET /api/festival/ongoing
     */
    @GetMapping("/ongoing")
    public ResponseEntity<List<FestivalResponseDto>> getOngoingFestivals() {
        try {
            List<FestivalEntity> allFestivals = searchFestivalService.getFestivalByTwoOpt("", "");
            List<FestivalEntity> ongoingFestivals = searchFestivalService.getFestivalGoing(allFestivals);
            List<FestivalResponseDto> response = ongoingFestivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("진행중인 축제 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 6. 진행 예정인 축제 조회
     * GET /api/festival/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<FestivalResponseDto>> getUpcomingFestivals() {
        try {
            List<FestivalEntity> allFestivals = searchFestivalService.getFestivalByTwoOpt("", "");
            List<FestivalEntity> upcomingFestivals = searchFestivalService.getFestivalAfter(allFestivals);
            List<FestivalResponseDto> response = upcomingFestivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("진행 예정 축제 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 7. 필터 + 상태별 조회 (진행중)
     * GET /api/festival/ongoing/filter?region={regionName}&category={categoryName}
     */
    @GetMapping("/ongoing/filter")
    public ResponseEntity<List<FestivalResponseDto>> getOngoingFestivalsByFilter(
            @RequestParam(required = false, defaultValue = "") String region,
            @RequestParam(required = false, defaultValue = "") String category) {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getOngoingFestivalsByFilter(region, category);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("진행중 축제 필터링 조회 실패: region={}, category={}", region, category, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 8. 필터 + 상태별 조회 (진행예정)
     * GET /api/festival/upcoming/filter?region={regionName}&category={categoryName}
     */
    @GetMapping("/upcoming/filter")
    public ResponseEntity<List<FestivalResponseDto>> getUpcomingFestivalsByFilter(
            @RequestParam(required = false, defaultValue = "") String region,
            @RequestParam(required = false, defaultValue = "") String category) {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getUpcomingFestivalsByFilter(region, category);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("진행 예정 축제 필터링 조회 실패: region={}, category={}", region, category, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 9. 특정 날짜의 축제 조회
     * GET /api/festival/date/{date}
     * 날짜 형식: yyyy-MM-dd (예: 2025-03-15)
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<FestivalResponseDto>> getFestivalsByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getFestivalByDate(date);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("특정 날짜 축제 조회 실패: {}", date, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 10. 특정 월의 축제 조회 (2025년 한정)
     * GET /api/festival/month/{month}
     * 월: 1-12
     */
    @GetMapping("/month/{month}")
    public ResponseEntity<List<FestivalResponseDto>> getFestivalsByMonth(
            @PathVariable int month) {
        try {
            if (month < 1 || month > 12) {
                return ResponseEntity.badRequest().build();
            }
            List<FestivalEntity> festivals = searchFestivalService.getFestivalByMonth(month);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("특정 월 축제 조회 실패: {}", month, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 11. 현재 월의 축제 조회
     * GET /api/festival/this-month
     */
    @GetMapping("/this-month")
    public ResponseEntity<List<FestivalResponseDto>> getFestivalsThisMonth() {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getFestivalThisMonth();
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("현재 월 축제 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 12. 인기 축제 TOP 5
     * GET /api/festival/popular
     */
    @GetMapping("/popular")
    public ResponseEntity<List<FestivalResponseDto>> getPopularFestivals() {
        try {
            List<FestivalEntity> festivals = searchFestivalService.getPopularFestivals();
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("인기 축제 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 13. 커스텀 개수 인기 축제
     * GET /api/festival/popular/{limit}
     */
    @GetMapping("/popular/{limit}")
    public ResponseEntity<List<FestivalResponseDto>> getPopularFestivals(
            @PathVariable int limit) {
        try {
            if (limit < 1 || limit > 100) {
                return ResponseEntity.badRequest().build();
            }
            List<FestivalEntity> festivals = searchFestivalService.getPopularFestivals(limit);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("커스텀 인기 축제 조회 실패: limit={}", limit, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 14. 키워드 검색
     * GET /api/festival/search?keyword={keyword}
     */
    @GetMapping("/search")
    public ResponseEntity<List<FestivalResponseDto>> searchFestivals(
            @RequestParam String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            List<FestivalEntity> festivals = searchFestivalService.searchFestivalsByKeyword(keyword);
            List<FestivalResponseDto> response = festivals.stream()
                .map(FestivalResponseDto::from)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("키워드 검색 실패: {}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 15. 축제 상세 조회 (상세 정보 포함)
     * GET /api/festival/{contentId}
     */
    @GetMapping("/{contentId}")
    public ResponseEntity<FestivalResponseDto> getFestivalDetail(@PathVariable String contentId) {
        try {
            // contentId로 축제 찾기 (Repository에 메서드 추가 필요)
            // 임시로 전체에서 찾기
            List<FestivalEntity> allFestivals = searchFestivalService.getFestivalByTwoOpt("", "");
            FestivalEntity festival = allFestivals.stream()
                .filter(f -> f.getContentId().equals(contentId))
                .findFirst()
                .orElse(null);
            
            if (festival == null) {
                return ResponseEntity.notFound().build();
            }
            
            FestivalResponseDto response = FestivalResponseDto.from(festival);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("축제 상세 조회 실패: contentId={}", contentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 16. 축제 상태만 확인
     * GET /api/festival/{contentId}/status
     */
    @GetMapping("/{contentId}/status")
    public ResponseEntity<String> getFestivalStatus(@PathVariable String contentId) {
        try {
            // contentId로 축제 찾기 (Repository에 메서드 추가 필요)
            // 임시로 전체에서 찾기
            List<FestivalEntity> allFestivals = searchFestivalService.getFestivalByTwoOpt("", "");
            FestivalEntity festival = allFestivals.stream()
                .filter(f -> f.getContentId().equals(contentId))
                .findFirst()
                .orElse(null);
            
            if (festival == null) {
                return ResponseEntity.notFound().build();
            }
            
            String status = searchFestivalService.getFestivalStatusString(festival);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("축제 상태 조회 실패: contentId={}", contentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}