package com.koreplan.controller.search;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.koreplan.dto.search.DataResponseDto;
import com.koreplan.service.search.FilterDataService;
import com.koreplan.data.dto.DataStatsDto; // 추가

import io.swagger.v3.oas.annotations.Operation;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.service.SearchDataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Builder;
import lombok.Data;

@RestController
@RequestMapping("/api/region-list")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
@CrossOrigin(origins = "http://localhost:5173")
public class RegionListRestController {

    private final FilterDataService filterDataService;
    private final SearchDataService searchDataService;

    /**
     * 데이터 리스트를 통계 데이터와 함께 DTO로 변환하는 헬퍼 메서드
     */
    private List<DataResponseDto> convertToDataResponseDtoWithStats(List<DataEntity> dataList) {
        if (dataList.isEmpty()) {
            return List.of();
        }
        
        // 1. contentId 목록 추출
        List<String> contentIds = dataList.stream()
            .map(DataEntity::getContentId)
            .collect(Collectors.toList());
        
        // 2. 통계 데이터 일괄 조회
        Map<String, DataStatsDto> statsMap = searchDataService.getBatchDataStatsByContentIds(contentIds);
        
        // 3. Entity + Stats 결합해서 DTO 변환
        return dataList.stream()
            .map(entity -> {
                DataStatsDto stats = statsMap.getOrDefault(entity.getContentId(), new DataStatsDto());

                return DataResponseDto.fromEntityWithStats(
                    entity,
                    (long) stats.getViewCount(),
                    (long) stats.getLikeCount(), 
                    stats.getRating(),
                    (long) stats.getReviewCount()
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * 지역리스트 페이지 초기화 - 헤더의 지역 버튼 클릭 시
     */
    @GetMapping("/init")
    @Operation(summary = "전국의 관광지(테마)를 가져옴",description ="지역 리스트에서 전국과 관광지가 선택된 상태의 데이터를 가져오는 api")
    public ResponseEntity<RegionListResponse> initRegionListPage() {
        log.info("지역리스트 페이지 초기화 요청");
        
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

            // 3. 초기 데이터: 전국의 관광지 (통계 데이터 포함)
            List<DataEntity> initialData = filterDataService.findAllDatasByTheme("관광지");
            List<DataResponseDto> dataList = convertToDataResponseDtoWithStats(initialData); // 변경된 부분
            
            RegionListResponse response = RegionListResponse.builder()
                    .regions(regionNames)
                    .themes(themes)
                    .wards(List.of()) // 초기에는 구/군 목록 없음
                    .selectedRegion("전국")
                    .selectedTheme("관광지")
                    .selectedWard("")
                    .dataList(dataList)
                    .totalCount(dataList.size())
                    .message("전국의 관광지 " + dataList.size() + "개를 표시합니다.")
                    .showWards(false) // 구/군 선택 UI 표시 여부
                    .build();

            log.info("지역리스트 페이지 초기화 완료. 지역: {}개, 데이터: {}개", regionNames.size(), dataList.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("지역리스트 페이지 초기화 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
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
     * 3단계 필터링: 지역 → 구/군 → 테마
     */
    @GetMapping("/filter")
    public ResponseEntity<FilterResponse> filterData(
            @RequestParam(defaultValue = "전국") String region,
            @RequestParam(defaultValue = "") List<String> ward,  // List로 변경
            @RequestParam(defaultValue = "관광지") String theme) {

        log.info("3단계 필터링 요청 - region: {}, ward: {}, theme: {}", region, ward, theme);

        try {
            List<DataEntity> dataList;
            String resultMessage;
            boolean showWards = false;

            // 1단계: 테마로 전체 데이터 조회
            dataList = filterDataService.findAllDatasByTheme(theme);
            log.info("테마 '{}' 전체 데이터: {}개", theme, dataList.size());

            // 2단계: 지역 및 구/군 필터링
            if (!"전국".equals(region)) {
                showWards = true; // 특정 지역 선택 시 구/군 선택 UI 표시

                if (!ward.isEmpty() && !ward.contains("전체")) {  // List 체크로 변경
                    // 구/군까지 선택된 경우
                    dataList = filterDataService.filterDatasByRegion(region, ward, dataList);
                    String wardNames = String.join(", ", ward);  // 구/군 이름들 조합
                    resultMessage = region + " " + wardNames + "의 " + theme + " " + dataList.size() + "개를 표시합니다.";
                    log.info("지역 '{}', 구/군 '{}' 필터링 후 데이터: {}개", region, ward, dataList.size());
                } else {
                    // 지역만 선택된 경우 (구/군은 전체)
                    dataList = filterDataService.filterDatasByRegion(region, List.of(), dataList);  // 빈 List 전달
                    resultMessage = region + " 전체의 " + theme + " " + dataList.size() + "개를 표시합니다.";
                    log.info("지역 '{}' 전체 필터링 후 데이터: {}개", region, dataList.size());
                }
            } else {
                // 전국 선택
                showWards = false;
                resultMessage = "전국의 " + theme + " " + dataList.size() + "개를 표시합니다.";
                log.info("전국 데이터 유지: {}개", dataList.size());
            }

            // 3단계: DTO 변환 (통계 데이터 포함) - 변경된 부분
            List<DataResponseDto> convertedData = convertToDataResponseDtoWithStats(dataList);

            FilterResponse response = FilterResponse.builder()
                    .selectedRegion(region)
                    .selectedWard(String.join(", ", ward))  // List를 String으로 변환
                    .selectedTheme(theme)
                    .dataList(convertedData)
                    .totalCount(convertedData.size())
                    .message(resultMessage)
                    .showWards(showWards)
                    .success(true)
                    .build();

            log.info("필터링 완료 - {}의 {} {} {}개", region, ward.isEmpty() ? "전체" : String.join(", ", ward), theme, convertedData.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("필터링 중 오류 발생", e);

            FilterResponse errorResponse = FilterResponse.builder()
                    .selectedRegion(region)
                    .selectedWard(ward.isEmpty() ? "" : String.join(", ", ward))  // 에러 응답도 수정
                    .selectedTheme(theme)
                    .dataList(List.of())
                    .totalCount(0)
                    .message("데이터 조회 중 오류가 발생했습니다.")
                    .showWards(false)
                    .success(false)
                    .build();

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 간단한 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Region List API is running!");
    }

    // ===== Response DTO Classes ===== (기존과 동일)

    @Data
    @Builder
    public static class RegionListResponse {
        private List<String> regions;
        private List<String> themes;
        private List<String> wards;
        private String selectedRegion;
        private String selectedTheme;
        private String selectedWard;
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
        private List<DataResponseDto> dataList;
        private Integer totalCount;
        private String message;
        private Boolean showWards;
        private Boolean success;
    }
}