package com.koreplan.data.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.data.dto.DataResponseDto;
import com.koreplan.data.dto.RegionDataResponseDto;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.service.RegionFilterDataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/region-data")
@RequiredArgsConstructor
@Slf4j
//@CrossOrigin(origins = "*") // 개발용 - 운영시에는 특정 도메인으로 제한
public class RegionDataController {

    private final RegionFilterDataService regionFilterDataService;

    /**
     * 모든 지역의 구/군 목록 조회
     * GET /api/region-data/regions/{regionName}/wards
     */
    @GetMapping("/regions/{regionName}/wards")
    public ResponseEntity<List<String>> getWardsByRegion(@PathVariable String regionName) {
        log.info("API 호출 - 구/군 목록 조회: {}", regionName);
        
        try {
            List<String> wards = regionFilterDataService.findWard(regionName);
            
            if (wards.isEmpty()) {
                log.warn("지역 '{}'의 구/군을 찾을 수 없습니다", regionName);
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(wards);
            
        } catch (Exception e) {
            log.error("구/군 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 지역별 데이터 조회
     * GET /api/region-data/regions/{regionName}/data
     */
    @GetMapping("/regions/{regionName}/data")
    public ResponseEntity<RegionDataResponseDto> getDataByRegion(@PathVariable String regionName) {
        log.info("API 호출 - 지역별 데이터 조회: {}", regionName);
        
        try {
            List<DataEntity> dataList = regionFilterDataService.findByRegion(regionName);
            
            if (dataList.isEmpty()) {
                log.warn("지역 '{}'의 데이터를 찾을 수 없습니다", regionName);
                return ResponseEntity.notFound().build();
            }
            
            // DTO로 변환
            RegionDataResponseDto response = RegionDataResponseDto.builder()
                .regionName(regionName)
                .totalCount(dataList.size())
                .dataList(dataList.stream()
                    .map(DataResponseDto::fromEntity)
                    .toList())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("지역별 데이터 조회 중 오류 발생: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 구/군별 데이터 조회
     * GET /api/region-data/regions/{regionName}/wards/{wardName}/data
     */
    @GetMapping("/regions/{regionName}/wards/{wardName}/data")
    public ResponseEntity<RegionDataResponseDto> getDataByWard(
            @PathVariable String regionName,
            @PathVariable String wardName) {
        
        log.info("API 호출 - 구/군별 데이터 조회: {} - {}", regionName, wardName);
        
        try {
            List<DataEntity> dataList = regionFilterDataService.findByWard(regionName, wardName);
            
            if (dataList.isEmpty()) {
                log.warn("지역 '{}', 구/군 '{}'의 데이터를 찾을 수 없습니다", regionName, wardName);
                return ResponseEntity.notFound().build();
            }
            
            // DTO로 변환
            RegionDataResponseDto response = RegionDataResponseDto.builder()
                .regionName(regionName)
                .wardName(wardName)
                .totalCount(dataList.size())
                .dataList(dataList.stream()
                    .map(DataResponseDto::fromEntity)
                    .toList())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("구/군별 데이터 조회 중 오류 발생: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 여러 조건으로 데이터 조회 (Query Parameter 방식)
     * GET /api/region-data/search?region=서울특별시&ward=종로구
     */
    @Transactional(readOnly = true)
    @GetMapping("/search")
    public ResponseEntity<RegionDataResponseDto> searchData(
            @RequestParam String region,
            @RequestParam(required = false) String ward) {
        
        log.info("API 호출 - 조건별 데이터 검색: region={}, ward={}", region, ward);
        
        try {
            List<DataEntity> dataList;
            
            // ward가 있으면 구/군별 조회, 없으면 지역별 조회
            if (ward != null && !ward.trim().isEmpty()) {
                dataList = regionFilterDataService.findByWard(region, ward);
            } else {
                dataList = regionFilterDataService.findByRegion(region);
            }
            
            if (dataList.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            RegionDataResponseDto response = RegionDataResponseDto.builder()
                .regionName(region)
                .wardName(ward)
                .totalCount(dataList.size())
                .dataList(dataList.stream()
                    .map(DataResponseDto::fromEntity)
                    .toList())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("데이터 검색 중 오류 발생: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 간단한 상태 확인용 API
     * GET /api/region-data/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Region Data API is running!");
    }
}
