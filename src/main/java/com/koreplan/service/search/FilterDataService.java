package com.koreplan.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.area.repository.WardCodeRepository;
import com.koreplan.category.entity.CategoryEntity;
import com.koreplan.category.repository.CategoryRepository;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.dto.search.DataResponseDto;
import com.koreplan.service.theme.ThemeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FilterDataService {
    private final CategoryRepository categoryRepository;
    private final DataRepository dataRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final WardCodeRepository wardCodeRepository;
    private final ThemeService themeService;

    //정렬 타입 열거형 추가
    public enum SortType {
        SCORE,          // 종합점수 (기본값)
        VIEW_COUNT,     // 조회수
        LIKE_COUNT,     // 찜수
        RATING,         // 평점
        REVIEW_COUNT    // 리뷰수
    }

    // ================== 기존 메서드들 (다른 기능에서 사용 중이므로 유지) ==================
    
    // 모든 시, 도 엔티티 갖고오는 메소드
    public List<RegionCodeEntity> findAllRegion() {
        return regionCodeRepository.findAll();
    }

    // 해당 시,도에 해당하는 구,군 이름 목록 반환
    @Transactional(readOnly = true)
    public List<String> findWard(String region) {
        log.info("=== findWard 시작 - 지역: {} ===", region);

        RegionCodeEntity regionEntity = getRegionEntity(region);
        if (regionEntity == null) {
            return new ArrayList<>();
        }

        List<String> names = regionEntity.getWardList().stream()
                .map(WardCodeEntity::getName)
                .collect(Collectors.toList());

        log.info("'{}'의 구/군 개수: {}", region, names.size());
        return names;
    }

    // 기존: 전체 데이터 조회 (다른 기능에서 사용 중 - Top5, 검색 등)
    public List<DataEntity> findAllDatasByTheme(String themeName, SortType sortType) {
        int themeNum = themeService.getThemeByName(themeName).getContentTypeId();
        
        switch (sortType) {
            case SCORE:
                return dataRepository.findByThemeOrderByScoreDescWithRegion(themeNum);
            case VIEW_COUNT:
                return dataRepository.findByThemeOrderByViewCountDescWithRegion(themeNum);
            case LIKE_COUNT:
                return dataRepository.findByThemeOrderByLikeCountDescWithRegion(themeNum);
            case RATING:
                return dataRepository.findByThemeOrderByRatingDescWithRegion(themeNum);
            case REVIEW_COUNT:
                return dataRepository.findByThemeOrderByReviewCountDescWithRegion(themeNum);
            default:
                return dataRepository.findByThemeOrderByScoreDescWithRegion(themeNum);
        }
    }

    // ================== 새로운 통합 페이징 메서드 ==================
    
    /**
     * 모든 조건을 통합하여 페이징 데이터 조회
     * @param region 지역명 ("전국" 또는 특정 지역)
     * @param ward 구/군 목록 (빈 리스트면 전체 지역)
     * @param theme 테마명 (필수)
     * @param sortType 정렬 타입
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return Page<DataEntity> 페이징된 결과
     */
    public Page<DataEntity> findDatasByConditionWithPaging(
            String region, List<String> ward, String theme, SortType sortType, int page, int size) {
        
        int themeNum = themeService.getThemeByName(theme).getContentTypeId();
        Pageable pageable = PageRequest.of(page, size);
        
        log.info("페이징 조회 시작 - 지역: {}, 구/군: {}, 테마: {}, 정렬: {}, 페이지: {}/{}", 
                region, ward, theme, sortType, page + 1, size);
        
        // 1. 전국 선택 시
        if ("전국".equals(region)) {
            return findByThemeWithPaging(themeNum, sortType, pageable);
        }
        
        // 2. 특정 지역 선택 시
        RegionCodeEntity regionEntity = regionCodeRepository.findByName(region);
        if (regionEntity == null) {
            log.warn("지역을 찾을 수 없습니다: {}", region);
            return Page.empty(pageable);
        }
        
        // 2-1. 구/군 선택 없음 (지역 전체)
        if (ward.isEmpty()) {
            return findByThemeAndRegionWithPaging(themeNum, regionEntity.getRegioncode(), sortType, pageable);
        }
        
        // 2-2. 구/군 선택됨
        List<Long> wardCodes = getWardCodes(regionEntity, ward);
        if (wardCodes.isEmpty()) {
            log.warn("유효한 구/군을 찾을 수 없습니다. 지역: {}, 구/군: {}", region, ward);
            return Page.empty(pageable);
        }
        
        return findByThemeAndRegionAndWardsWithPaging(themeNum, regionEntity.getRegioncode(), wardCodes, sortType, pageable);
    }
    
    // ================== Private 헬퍼 메서드들 ==================
    
    /**
     * 전국 + 테마 페이징 조회
     */
    private Page<DataEntity> findByThemeWithPaging(int theme, SortType sortType, Pageable pageable) {
        switch (sortType) {
            case SCORE:
                return dataRepository.findByThemeOrderByScoreDescWithRegionPaged(theme, pageable);
            case VIEW_COUNT:
                return dataRepository.findByThemeOrderByViewCountDescWithRegionPaged(theme, pageable);
            case LIKE_COUNT:
                return dataRepository.findByThemeOrderByLikeCountDescWithRegionPaged(theme, pageable);
            case RATING:
                return dataRepository.findByThemeOrderByRatingDescWithRegionPaged(theme, pageable);
            case REVIEW_COUNT:
                return dataRepository.findByThemeOrderByReviewCountDescWithRegionPaged(theme, pageable);
            default:
                return dataRepository.findByThemeOrderByScoreDescWithRegionPaged(theme, pageable);
        }
    }
    
    /**
     * 지역 + 테마 페이징 조회
     */
    private Page<DataEntity> findByThemeAndRegionWithPaging(int theme, Long regionCode, SortType sortType, Pageable pageable) {
        switch (sortType) {
            case SCORE:
                return dataRepository.findByThemeAndRegionOrderByScoreDescWithRegionPaged(theme, regionCode, pageable);
            case VIEW_COUNT:
                return dataRepository.findByThemeAndRegionOrderByViewCountDescWithRegionPaged(theme, regionCode, pageable);
            case LIKE_COUNT:
                return dataRepository.findByThemeAndRegionOrderByLikeCountDescWithRegionPaged(theme, regionCode, pageable);
            case RATING:
                return dataRepository.findByThemeAndRegionOrderByRatingDescWithRegionPaged(theme, regionCode, pageable);
            case REVIEW_COUNT:
                return dataRepository.findByThemeAndRegionOrderByReviewCountDescWithRegionPaged(theme, regionCode, pageable);
            default:
                return dataRepository.findByThemeAndRegionOrderByScoreDescWithRegionPaged(theme, regionCode, pageable);
        }
    }
    
    /**
     * 지역 + 구/군 + 테마 페이징 조회
     */
    private Page<DataEntity> findByThemeAndRegionAndWardsWithPaging(int theme, Long regionCode, List<Long> wardCodes, SortType sortType, Pageable pageable) {
        switch (sortType) {
            case SCORE:
                return dataRepository.findByThemeAndRegionAndWardsOrderByScoreDescWithRegionPaged(theme, regionCode, wardCodes, pageable);
            case VIEW_COUNT:
                return dataRepository.findByThemeAndRegionAndWardsOrderByViewCountDescWithRegionPaged(theme, regionCode, wardCodes, pageable);
            case LIKE_COUNT:
                return dataRepository.findByThemeAndRegionAndWardsOrderByLikeCountDescWithRegionPaged(theme, regionCode, wardCodes, pageable);
            case RATING:
                return dataRepository.findByThemeAndRegionAndWardsOrderByRatingDescWithRegionPaged(theme, regionCode, wardCodes, pageable);
            case REVIEW_COUNT:
                return dataRepository.findByThemeAndRegionAndWardsOrderByReviewCountDescWithRegionPaged(theme, regionCode, wardCodes, pageable);
            default:
                return dataRepository.findByThemeAndRegionAndWardsOrderByScoreDescWithRegionPaged(theme, regionCode, wardCodes, pageable);
        }
    }
    
    /**
     * 지역 엔티티 조회 헬퍼
     */
    private RegionCodeEntity getRegionEntity(String regionName) {
        if (regionName == null || regionName.trim().isEmpty()) {
            log.warn("지역명이 비어있습니다");
            return null;
        }

        RegionCodeEntity regionEntity = regionCodeRepository.findByName(regionName.trim());
        if (regionEntity == null) {
            log.warn("존재하지 않는 지역명: {}", regionName);
        }
        return regionEntity;
    }
    
    /**
     * 구/군 이름을 코드로 변환하는 헬퍼
     */
    private List<Long> getWardCodes(RegionCodeEntity regionEntity, List<String> wardNames) {
        return regionEntity.getWardList().stream()
                .filter(ward -> wardNames.contains(ward.getName()))
                .map(WardCodeEntity::getWardcode)
                .collect(Collectors.toList());
    }

    // ================== 기존 다른 메서드들 (유지) ==================
    
    // 지원하는 테마 목록 반환
    public List<String> getSupportedThemes() {
        return List.of("관광지", "문화시설", "축제공연행사", "여행코스", "레포츠", "숙박", "쇼핑", "음식점");
    }

    // DTO 변환
    public List<DataResponseDto> convertToDataResponseDto(List<DataEntity> dataList) {
        return dataList.stream()
                .map(DataResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // 입력받은 카테고리 별 하위 카테고리 리스트 반환하는 함수 (기존 유지)
    public List<String> findSubCategoryName(String categoryname, int level) {
        // 기존 로직 그대로 유지
        List<String> subcategories = new ArrayList<>();
        
        if (level == 1) {
            List<CategoryEntity> categories = categoryRepository.findByC1Name(categoryname);
            for (CategoryEntity category : categories) {
                if (!subcategories.contains(category.getC2Name())) {
                    subcategories.add(category.getC2Name());
                }
            }
        } else if (level == 2) {
            List<CategoryEntity> categories = categoryRepository.findByC2Name(categoryname);
            for (CategoryEntity category : categories) {
                if (!subcategories.contains(category.getC3Name())) {
                    subcategories.add(category.getC3Name());
                }
            }
        } else {
            List<CategoryEntity> categories = categoryRepository.findAll();
            for (CategoryEntity category : categories) {
                if (!subcategories.contains(category.getC1Name())) {
                    subcategories.add(category.getC1Name());
                }
            }
        }
        
        return subcategories;
    }

    
}