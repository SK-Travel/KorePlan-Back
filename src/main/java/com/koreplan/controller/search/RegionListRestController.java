package com.koreplan.controller.search;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.service.SearchDataService;
import com.koreplan.dto.search.DataResponseDto;
import com.koreplan.service.search.FilterDataService;
import com.koreplan.service.search.FilterDataService.SortType; 

import io.swagger.v3.oas.annotations.Operation;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/region-list")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class RegionListRestController {

    private final FilterDataService filterDataService;
    private final SearchDataService searchDataService;
    
    /**
     * String을 SortType으로 변환하는 헬퍼 메서드
     */
    private SortType parseSortType(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return SortType.SCORE; // 기본값
        }
        
        try {
            return SortType.valueOf(sortParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 정렬 타입: {}. 기본값(SCORE) 사용", sortParam);
            return SortType.SCORE;
        }
    }

    /**
     * 지역리스트 페이지 초기화 - 헤더의 지역 버튼 클릭 시 (정렬 파라미터 추가)
     */
    @GetMapping("/init")
    @Operation(summary = "전국의 관광지(테마)를 가져옴",description ="지역 리스트에서 전국과 관광지가 선택된 상태의 데이터를 가져오는 api")
    public ResponseEntity<RegionListResponse> initRegionListPage(
            @RequestParam(defaultValue = "SCORE") String sort) { // ✅ 정렬 파라미터 추가
        
        log.info("지역리스트 페이지 초기화 요청 - 정렬: {}", sort);
        
        try {
            // 1. 전체 지역 목록 조회 (전국 + 모든 시/도)
            List<RegionCodeEntity> regions = filterDataService.findAllRegion();
            List<String> regionNames = regions.stream()
                    .map(RegionCodeEntity::getName)
                    .collect(Collectors.toList());
            
            // 맨 앞에 "전국" 추가
            regionNames.add(0, "전국");

            // 2. 테마 목록 
            List<String> themes = List.of("관광지", "숙박", "음식점", "쇼핑", "문화시설", "레포츠", "축제공연행사");

            // 3. 정렬 옵션 목록 ✅ 추가
            List<String> sortOptions = List.of("SCORE", "VIEW_COUNT", "LIKE_COUNT", "RATING", "REVIEW_COUNT");

            // 4. 초기 데이터: 전국의 관광지 (정렬 적용) ✅ 수정
            SortType sortType = parseSortType(sort);
            List<DataEntity> initialData = filterDataService.findAllDatasByTheme("관광지", sortType);
            List<DataResponseDto> dataList = filterDataService.convertToDataResponseDto(initialData);
            
            RegionListResponse response = RegionListResponse.builder()
                    .regions(regionNames)
                    .themes(themes)
                    .sortOptions(sortOptions) // ✅ 추가
                    .wards(List.of()) // 초기에는 구/군 목록 없음
                    .selectedRegion("전국")
                    .selectedTheme("관광지")
                    .selectedWard("")
                    .selectedSort(sort) // ✅ 추가
                    .dataList(dataList)
                    .totalCount(dataList.size())
                    .message("전국의 관광지 " + dataList.size() + "개를 " + getSortDisplayName(sortType) + " 순으로 표시합니다.")
                    .showWards(false) // 구/군 선택 UI 표시 여부
                    .build();

            log.info("지역리스트 페이지 초기화 완료. 지역: {}개, 데이터: {}개, 정렬: {}", regionNames.size(), dataList.size(), sortType);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("지역리스트 페이지 초기화 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 정렬 타입의 한글 표시명을 반환하는 헬퍼 메서드
     */
    private String getSortDisplayName(SortType sortType) {
        return switch (sortType) {
            case SCORE -> "종합점수";
            case VIEW_COUNT -> "조회수";
            case LIKE_COUNT -> "찜수";
            case RATING -> "평점";
            case REVIEW_COUNT -> "리뷰수";
        };
    }

    /**
     * 특정 지역의 구/군 목록 조회 (기존 findWard 메서드 활용)
     */
    @GetMapping("/regions/{regionName}/wards")
    public ResponseEntity<WardListResponse> getWardsByRegion(@PathVariable String regionName) {
        log.info("구/군 목록 조회 요청: {}", regionName);
        
        try {
            if ("전국".equals(regionName)) {
                return ResponseEntity.ok(WardListResponse.builder()
                        .regionName(regionName)
                        .wards(List.of())
                        .totalCount(0)
                        .message("전국에는 구/군 선택이 불가능합니다.")
                        .build());
            }

            // 기존 findWard 메서드 사용 - 훨씬 간단!
            List<String> wardNames = filterDataService.findWard(regionName);
            
            if (wardNames.isEmpty()) {
                log.warn("지역 '{}'의 구/군을 찾을 수 없습니다", regionName);
                return ResponseEntity.notFound().build();
            }

            WardListResponse response = WardListResponse.builder()
                    .regionName(regionName)
                    .wards(wardNames)
                    .totalCount(wardNames.size())
                    .message(regionName + "의 구/군 " + wardNames.size() + "개를 조회했습니다.")
                    .build();

            log.info("구/군 목록 조회 완료: {} - {}개", regionName, wardNames.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("구/군 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 4단계 필터링: 지역 → 구/군 → 테마 → 정렬 ✅ 수정
     */
    /**
     * 4단계 필터링: 지역 → 구/군 → 테마 → 정렬 + 페이징 ✅ 수정
     */
    @GetMapping("/filter")
    public ResponseEntity<FilterResponse> filterData(
            @RequestParam(defaultValue = "전국") String region,
            @RequestParam(defaultValue = "") List<String> ward,
            @RequestParam(defaultValue = "관광지") String theme,
            @RequestParam(defaultValue = "SCORE") String sort,
            @RequestParam(defaultValue = "0") int page,        // 페이지 번호 (0부터 시작)
            @RequestParam(defaultValue = "12") int size) {     // 페이지 크기

        log.info("필터링 + 페이징 요청 - region: {}, ward: {}, theme: {}, sort: {}, page: {}, size: {}", 
                 region, ward, theme, sort, page, size);

        try {
            SortType sortType = parseSortType(sort);
            
            // ✅ 모든 경우를 하나의 Service 메서드로 통합 처리!
            Page<DataEntity> pagedData = filterDataService.findDatasByConditionWithPaging(
                    region, ward, theme, sortType, page, size);
            
            // DTO 변환
            List<DataResponseDto> dataList = filterDataService.convertToDataResponseDto(pagedData.getContent());
            
            // 메시지 생성
            String message = generateMessage(region, ward, theme, sortType, pagedData.getTotalElements());
            
            // ✅ 깔끔한 응답 생성
            FilterResponse response = FilterResponse.builder()
                    .selectedRegion(region)
                    .selectedWard(String.join(", ", ward))
                    .selectedTheme(theme)
                    .selectedSort(sort)
                    .dataList(dataList)
                    .totalCount((int) pagedData.getTotalElements())
                    .currentPage(pagedData.getNumber())
                    .totalPages(pagedData.getTotalPages())
                    .pageSize(pagedData.getSize())
                    .hasNext(pagedData.hasNext())
                    .hasPrevious(pagedData.hasPrevious())
                    .message(message)
                    .showWards(!"전국".equals(region))
                    .success(true)
                    .build();

            log.info("필터링 완료 - 전체: {}개, 현재 페이지: {}개, 페이지: {}/{}", 
                    pagedData.getTotalElements(), dataList.size(), page + 1, pagedData.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("필터링 중 오류 발생", e);
            return ResponseEntity.ok(createErrorResponse(region, ward, theme, sort, page, size));
        }
    }

    
    

    /**
     * ✅ Top5 데이터 조회 API (메인 페이지용)
     */
    @GetMapping("/top5")
    @Operation(summary = "점수 기준 상위 5개 데이터 조회", description = "메인 페이지에서 사용할 전체 데이터 중 점수가 높은 상위 5개 데이터를 반환")
    public ResponseEntity<Top5Response> getTop5Data() {
        log.info("메인 페이지 Top5 데이터 조회 요청");
        
        try {
            // SearchDataService의 Top5 메서드 활용
            
            List<DataResponseDto> top5DataDto = searchDataService.getAsDto(searchDataService.getTop5PlacesByScore());
            // DTO 변환
            
            
            Top5Response response = Top5Response.builder()
                    .dataList(top5DataDto)
                    .totalCount(top5DataDto.size())
                    .message("점수 기준 상위 " + top5DataDto.size() + "개 데이터를 조회했습니다.")
                    .success(true)
                    .build();
            
            log.info("메인 페이지 Top5 데이터 조회 완료: {}개", top5DataDto.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Top5 데이터 조회 중 오류 발생", e);
            
            Top5Response errorResponse = Top5Response.builder()
                    .dataList(List.of())
                    .totalCount(0)
                    .message("Top5 데이터 조회 중 오류가 발생했습니다.")
                    .success(false)
                    .build();
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    @GetMapping("/top5-hotels")
    @Operation(summary = "점수 기준 상위 5개 숙박 데이터 조회", description = "메인 페이지에서 사용할 숙박 데이터 중 점수가 높은 상위 5개 데이터를 반환")
    public ResponseEntity<Top5Response> getTop5Hotels() {
        log.info("메인 페이지 Top5 숙박 데이터 조회 요청");

        try {
            // SearchDataService의 Top5 숙박 메서드 활용
            List<DataResponseDto> top5HotelsDto = searchDataService.getAsDto(searchDataService.getTop5HotelsByScore());
            
            Top5Response response = Top5Response.builder()
                    .dataList(top5HotelsDto)
                    .totalCount(top5HotelsDto.size())
                    .message("점수 기준 상위 " + top5HotelsDto.size() + "개 숙박 데이터를 조회했습니다.")
                    .success(true)
                    .build();

            log.info("메인 페이지 Top5 숙박 데이터 조회 완료: {}개", top5HotelsDto.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Top5 숙박 데이터 조회 중 오류 발생", e);

            Top5Response errorResponse = Top5Response.builder()
                    .dataList(List.of())
                    .totalCount(0)
                    .message("Top5 숙박 데이터 조회 중 오류가 발생했습니다.")
                    .success(false)
                    .build();

            return ResponseEntity.ok(errorResponse);
        }
    }
    //리뷰페이지에서 쓸 단일데이터 뽑아오기.
    @GetMapping("/{contentId}/one-data")
    public ResponseEntity<DataResponseDto> getOneData(@PathVariable String contentId) {
        log.info("=== getOneData 호출됨. contentId: {} ===", contentId);
        
        try {
            // Service에서 DTO까지 변환해서 받아오기
            DataResponseDto data = searchDataService.getDataResponseDtoByContentId(contentId);
            
            if (data == null) {
                log.warn("데이터를 찾을 수 없습니다. contentId: {}", contentId);
                return ResponseEntity.notFound().build();
            }
            
            log.info("데이터 조회 성공: {}", data.getTitle());
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            log.error("데이터 조회 중 오류 발생. contentId: {}", contentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    /**
     * 메시지 생성 헬퍼 메서드
     */
    private String generateMessage(String region, List<String> ward, String theme, SortType sortType, long totalElements) {
        String sortDisplayName = getSortDisplayName(sortType);
        
        if ("전국".equals(region)) {
            return String.format("전국의 %s %,d개를 %s 순으로 표시합니다.", 
                                theme, totalElements, sortDisplayName);
        } else if (ward.isEmpty() || ward.contains("전체")) {
            return String.format("%s 전체의 %s %,d개를 %s 순으로 표시합니다.", 
                                region, theme, totalElements, sortDisplayName);
        } else {
            String wardNames = String.join(", ", ward);
            return String.format("%s %s의 %s %,d개를 %s 순으로 표시합니다.", 
                                region, wardNames, theme, totalElements, sortDisplayName);
        }
    }

    /**
     * 에러 응답 생성 헬퍼 메서드
     */
    private FilterResponse createErrorResponse(String region, List<String> ward, String theme, String sort, int page, int size) {
        return FilterResponse.builder()
                .selectedRegion(region)
                .selectedWard(String.join(", ", ward))
                .selectedTheme(theme)
                .selectedSort(sort)
                .dataList(List.of())
                .totalCount(0)
                .currentPage(page)
                .totalPages(0)
                .pageSize(size)
                .hasNext(false)
                .hasPrevious(false)
                .message("데이터 조회 중 오류가 발생했습니다.")
                .showWards(false)
                .success(false)
                .build();
    }
    

    /**
     * 간단한 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Region List API is running!");
    }

    // ===== Response DTO Classes ===== ✅ 수정

    @Data
    @Builder
    public static class Top5Response {
        private List<DataResponseDto> dataList;
        private Integer totalCount;
        private String message;
        private Boolean success;
    }

    @Data
    @Builder
    public static class RegionListResponse {
        private List<String> regions;
        private List<String> themes;
        private List<String> sortOptions; // ✅ 추가
        private List<String> wards;
        private String selectedRegion;
        private String selectedTheme;
        private String selectedWard;
        private String selectedSort; // ✅ 추가
        private List<DataResponseDto> dataList;
        private Integer totalCount;
        private String message;
        private Boolean showWards;
    }

    @Data
    @Builder
    public static class WardListResponse {
        private String regionName;
        private List<String> wards;
        private Integer totalCount;
        private String message;
    }

    @Data
    @Builder
    public static class FilterResponse {
        private String selectedRegion;
        private String selectedWard;
        private String selectedTheme;
        private String selectedSort;
        private List<DataResponseDto> dataList;
        private Integer totalCount; // 전체 데이터 개수
        
        // 페이징 정보
        private Integer currentPage; // 현재 페이지 (0부터 시작)
        private Integer totalPages; // 전체 페이지 수
        private Integer pageSize; // 페이지 크기
        private Boolean hasNext; // 다음 페이지 존재 여부
        private Boolean hasPrevious; // 이전 페이지 존재 여부
        
        private String message;
        private Boolean showWards;
        private Boolean success;
    }
}