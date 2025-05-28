package com.koreplan.controller.search;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.search.DataResponseDto;
import com.koreplan.service.search.FilterDataService;
import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.data.entity.DataEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Builder;
import lombok.Data;

@RestController
@RequestMapping("/api/region-list")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RegionListRestController {

    private final FilterDataService filterDataService;

    /**
     * 지역리스트 페이지 초기화 - 헤더의 지역 버튼 클릭 시
     * GET /api/region-list/init
     * 
     * 초기 상태: 전국 + 관광지 선택됨
     */
    @GetMapping("/init")
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

            // 3. 초기 데이터: 전국의 관광지
            List<DataEntity> initialData = filterDataService.findAllDatasByTheme("관광지");
            List<DataResponseDto> dataList = initialData.stream()
                    .map(DataResponseDto::fromEntity)
                    .collect(Collectors.toList());

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
     * GET /api/region-list/regions/{regionName}/wards
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
            
            // 맨 앞에 "전체" 추가 (해당 지역 전체 선택용)
            wardNames.add(0, "전체");

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
     * GET /api/region-list/filter?region=서울특별시&ward=종로구&theme=관광지
     */
    @GetMapping("/filter")
    public ResponseEntity<FilterResponse> filterData(
            @RequestParam(defaultValue = "전국") String region,
            @RequestParam(defaultValue = "") String ward,
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
                
                if (ward != null && !ward.trim().isEmpty() && !"전체".equals(ward)) {
                    // 구/군까지 선택된 경우
                    dataList = filterDataService.filterDatasByRegion(region, ward, dataList);
                    resultMessage = region + " " + ward + "의 " + theme + " " + dataList.size() + "개를 표시합니다.";
                    log.info("지역 '{}', 구/군 '{}' 필터링 후 데이터: {}개", region, ward, dataList.size());
                } else {
                    // 지역만 선택된 경우 (구/군은 전체)
                    dataList = filterDataService.filterDatasByRegion(region, "", dataList);
                    resultMessage = region + " 전체의 " + theme + " " + dataList.size() + "개를 표시합니다.";
                    log.info("지역 '{}' 전체 필터링 후 데이터: {}개", region, dataList.size());
                }
            } else {
                // 전국 선택
                showWards = false;
                resultMessage = "전국의 " + theme + " " + dataList.size() + "개를 표시합니다.";
                log.info("전국 데이터 유지: {}개", dataList.size());
            }

            // 3단계: DTO 변환
            List<DataResponseDto> convertedData = dataList.stream()
                    .map(DataResponseDto::fromEntity)
                    .collect(Collectors.toList());

            FilterResponse response = FilterResponse.builder()
                    .selectedRegion(region)
                    .selectedWard(ward)
                    .selectedTheme(theme)
                    .dataList(convertedData)
                    .totalCount(convertedData.size())
                    .message(resultMessage)
                    .showWards(showWards)
                    .success(true)
                    .build();

            log.info("필터링 완료 - {}의 {} {} {}개", region, ward.isEmpty() ? "전체" : ward, theme, convertedData.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("필터링 중 오류 발생", e);
            
            FilterResponse errorResponse = FilterResponse.builder()
                    .selectedRegion(region)
                    .selectedWard(ward)
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
     * GET /api/region-list/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Region List API is running!");
    }

    // ===== Response DTO Classes =====

    /**
     * 지역리스트 페이지 초기화 응답
     */
    @Data
    @Builder
    public static class RegionListResponse {
        private List<String> regions;          // ["전국", "서울특별시", "부산광역시", ...]
        private List<String> themes;           // ["관광지", "숙박", "음식점", ...]
        private List<String> wards;            // ["전체", "종로구", "중구", ...] (특정 지역 선택 시)
        private String selectedRegion;         // "전국"
        private String selectedTheme;          // "관광지"
        private String selectedWard;           // ""
        private List<DataResponseDto> dataList; // 데이터
        private Integer totalCount;            // 데이터 개수
        private String message;                // 상태 메시지
        private Boolean showWards;             // 구/군 선택 UI 표시 여부
    }

    /**
     * 구/군 목록 조회 응답
     */
    @Data
    @Builder
    public static class WardListResponse {
        private String regionName;             // 지역명
        private List<String> wards;            // ["전체", "종로구", "중구", ...]
        private Integer totalCount;            // 구/군 개수
        private String message;                // 메시지
    }

    /**
     * 필터링 결과 응답
     */
    @Data
    @Builder
    public static class FilterResponse {
        private String selectedRegion;         // 선택된 지역
        private String selectedWard;           // 선택된 구/군
        private String selectedTheme;          // 선택된 테마
        private List<DataResponseDto> dataList; // 필터링된 데이터
        private Integer totalCount;            // 데이터 개수
        private String message;                // 결과 메시지
        private Boolean showWards;             // 구/군 선택 UI 표시 여부
        private Boolean success;               // 성공 여부
    }
}