package com.koreplan.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // 모든 시, 도 엔티티 갖고오는 메소드
    public List<RegionCodeEntity> findAllRegion() {
        List<RegionCodeEntity> regions = regionCodeRepository.findAll();
        return regions;
    }

    /**
     * 해당 시,도에 해당하는 구,군 이름 목록 반환
     */
    @Transactional(readOnly = true)
    public List<String> findWard(String region) {
        log.info("=== findWard 시작 - 지역: {} ===", region);

        List<String> names = new ArrayList<>();

        // 지역 Entity 조회
        RegionCodeEntity regionEntity = getRegionEntity(region);
        if (regionEntity == null) {
            return names;
        }

        // 해당 지역의 구/군 목록 조회 및 이름 추출
        List<WardCodeEntity> wardList = regionEntity.getWardList();
        for (WardCodeEntity ward : wardList) {
            names.add(ward.getName());
        }

        log.info("'{}'의 구/군 개수: {}", region, names.size());
        log.info("=== findWard 종료 ===");

        return names;
    }

    // ✅ 기존 메서드 - 기본값으로 Score 정렬 사용
    public List<DataEntity> findAllDatasByTheme(String themeName) {
        return findAllDatasByTheme(themeName, SortType.SCORE);
    }

    // ✅ 기존 메서드 수정 - LazyInitializationException 해결을 위해 JOIN FETCH 사용
    public List<DataEntity> findAllDatasByTheme(String themeName, SortType sortType) {
        int themeNum = themeService.getThemeByName(themeName).getContentTypeId();
        
        List<DataEntity> results;
        
        // ✅ Switch문으로 정렬 타입에 따라 JOIN FETCH가 포함된 Repository 메서드 호출
        switch (sortType) {
            case SCORE:
                results = dataRepository.findByThemeOrderByScoreDescWithRegion(themeNum);
                log.info("테마 '{}' 데이터를 종합점수 순으로 정렬 (JOIN FETCH)", themeName);
                break;
                
            case VIEW_COUNT:
                results = dataRepository.findByThemeOrderByViewCountDescWithRegion(themeNum);
                log.info("테마 '{}' 데이터를 조회수 순으로 정렬 (JOIN FETCH)", themeName);
                break;
                
            case LIKE_COUNT:
                results = dataRepository.findByThemeOrderByLikeCountDescWithRegion(themeNum);
                log.info("테마 '{}' 데이터를 찜수 순으로 정렬 (JOIN FETCH)", themeName);
                break;
                
            case RATING:
                results = dataRepository.findByThemeOrderByRatingDescWithRegion(themeNum);
                log.info("테마 '{}' 데이터를 평점 순으로 정렬 (JOIN FETCH)", themeName);
                break;
                
            case REVIEW_COUNT:
                results = dataRepository.findByThemeOrderByReviewCountDescWithRegion(themeNum);
                log.info("테마 '{}' 데이터를 리뷰수 순으로 정렬 (JOIN FETCH)", themeName);
                break;
                
            default:
                // 기본값은 종합점수 순
                results = dataRepository.findByThemeOrderByScoreDescWithRegion(themeNum);
                log.info("테마 '{}' 데이터를 기본(종합점수) 순으로 정렬 (JOIN FETCH)", themeName);
                break;
        }
        
        log.info("정렬 타입: {}, 결과 개수: {}", sortType, results.size());
        return results;
    }

    //전체 결과에서 지역에 대한 필터링 메서드
    public List<DataEntity> filterDatasByRegion(String region, List<String> ward, List<DataEntity> preResults) {
        // 필터링된 결과를 저장할 DataEntity형 리스트 선언
        List<DataEntity> results = new ArrayList<>();

        // 상위 지역(시/도)이 입력되지 않은 경우
        if (region.isEmpty()) {
            log.info("상위 지역이 선택되지 않았습니다.");
            // 이전 결과를 그대로 반환 (필터링하지 않음)
            return preResults;
        }

        // 상위 지역만 넘어왔을 경우 (구/군 정보 없음)
        else if (ward.isEmpty()) {
            // 지역명으로 지역 엔티티 조회
            RegionCodeEntity regionEntity = regionCodeRepository.findByName(region);

            // null 체크 추가
            if (regionEntity == null) {
                log.warn("지역을 찾을 수 없습니다: {}", region);
                return results; // 빈 리스트 반환
            }

            // 이전 결과를 순회하면서 해당 지역의 데이터만 필터링
            for (DataEntity data : preResults) {
                // null 체크 및 ID 값으로 비교 (객체 참조 비교 문제 해결)
                if (data.getRegionCodeEntity() != null
                        && regionEntity.getRegioncode().equals(data.getRegionCodeEntity().getRegioncode())) {
                    // 조건에 맞는 데이터를 결과 리스트에 추가
                    results.add(data);
                }
            }
            log.info("지역 필터링 결과 개수: {}", results.size());
            return results;
        }

        // 상위 지역과 하위 지역(구/군) 모두 넘어온 경우
        else {
            // 상위 지역의 지역 엔티티 조회
            RegionCodeEntity regionEntity = regionCodeRepository.findByName(region);

            // null 체크 추가
            if (regionEntity == null) {
                log.warn("지역을 찾을 수 없습니다: {}", region);
                return results; // 빈 리스트 반환
            }

            // 해당 상위 지역의 모든 하위 지역(구/군) 목록 먼저 가져오기
            List<WardCodeEntity> wardList = regionEntity.getWardList();

            // ward를 순회하면서 해당하는 구/군을 찾아야함.
            // 해당 ward엔터티들을 저장할 리스트 변수 선언
            List<WardCodeEntity> selectedWards = new ArrayList<>();
            for (String a : ward) {
                // 변수 a에 들어가는 값 = 사용자가 클릭한 지역구 값 중 하나
                // a에 해당하는 ward엔티티 찾기. wardList에서
                WardCodeEntity wardEntity = wardList.stream().filter(w -> w.getName().equals(a)).findFirst()
                        .orElse(null);
                // 해당하는 값만 selected 리스트에 들어가게끔한다.
                if (wardEntity != null) {
                    selectedWards.add(wardEntity);
                }
            }

            // 해당 지역에 요청받은 구/군이 없는 경우
            if (selectedWards.isEmpty()) {
                log.warn("구/군 '{}'이 지역 '{}'에 속하지 않습니다.", ward, region);
                return results;
            }
            Set<Long> selectedWardCodes = selectedWards.stream().map(WardCodeEntity::getWardcode)
                    .collect(Collectors.toSet());

            // O(1) 검색으로 필터링
            for (DataEntity data : preResults) {
                if (data.getRegionCodeEntity() != null && data.getWardCodeEntity() != null
                        && regionEntity.getRegioncode().equals(data.getRegionCodeEntity().getRegioncode())
                        && selectedWardCodes.contains(data.getWardCodeEntity().getWardcode())) {
                    results.add(data);
                }
            }

            log.info("지역+구군 필터링 결과 개수: {}", results.size());
            return results;
        }
    }

    // 입력받은 카테고리 별 하위 카테고리 리스트 반환하는 함수
    public List<String> findSubCategoryName(String categoryname, int level) {
        // 현재 상태에 관한 하위 카테고리 이름들을 저장할 리스트 선언
        List<String> subcategories = new ArrayList<>();

        log.info("=== findSubCategoryName 시작 ===");
        log.info("입력 파라미터 - categoryname: {}, level: {}", categoryname, level);

        // 레벨이 1일 때: 최상위 카테고리 선택됨 -> 중간 카테고리 이름들 반환
        if (level == 1) {
            log.info("레벨 1 처리: 최상위 카테고리 '{}' 의 중간 카테고리들 조회", categoryname);
            List<CategoryEntity> categories = categoryRepository.findByC1Name(categoryname);
            log.info("DB에서 조회된 카테고리 개수: {}", categories.size());

            for (CategoryEntity category : categories) {
                log.debug("현재 처리 중인 카테고리: C1={}, C2={}, C3={}", category.getC1Name(), category.getC2Name(),
                        category.getC3Name());

                // 이미 리스트에 있으면 건너뛰기 (중복 방지)
                if (subcategories.contains(category.getC2Name())) {
                    log.debug("중복된 C2 카테고리 건너뛰기: {}", category.getC2Name());
                    continue;
                }
                subcategories.add(category.getC2Name());
                log.debug("C2 카테고리 추가됨: {}", category.getC2Name());
            }
            log.info("레벨 1 처리 완료. 반환할 중간 카테고리 개수: {}, 목록: {}", subcategories.size(), subcategories);
            return subcategories;
        }

        // 레벨이 2일 때: 중간 카테고리 선택됨 -> 최하위 카테고리 이름들 반환
        if (level == 2) {
            log.info("레벨 2 처리: 중간 카테고리 '{}' 의 최하위 카테고리들 조회", categoryname);
            List<CategoryEntity> categories = categoryRepository.findByC2Name(categoryname);
            log.info("DB에서 조회된 카테고리 개수: {}", categories.size());

            for (CategoryEntity category : categories) {
                log.debug("현재 처리 중인 카테고리: C1={}, C2={}, C3={}", category.getC1Name(), category.getC2Name(),
                        category.getC3Name());

                // 이미 리스트에 있으면 건너뛰기 (중복 방지)
                if (subcategories.contains(category.getC3Name())) {
                    log.debug("중복된 C3 카테고리 건너뛰기: {}", category.getC3Name());
                    continue;
                }
                subcategories.add(category.getC3Name());
                log.debug("C3 카테고리 추가됨: {}", category.getC3Name());
            }
            log.info("레벨 2 처리 완료. 반환할 최하위 카테고리 개수: {}, 목록: {}", subcategories.size(), subcategories);
            return subcategories;
        }

        // 레벨이 0일 때: 아무것도 선택 안됨 -> 최상위 카테고리 이름들 반환
        log.info("레벨 0 처리: 모든 최상위 카테고리들 조회");
        List<CategoryEntity> categories = categoryRepository.findAll();
        log.info("DB에서 조회된 전체 카테고리 개수: {}", categories.size());

        for (CategoryEntity category : categories) {
            log.debug("현재 처리 중인 카테고리: C1={}, C2={}, C3={}", category.getC1Name(), category.getC2Name(),
                    category.getC3Name());

            // 이미 리스트에 있으면 건너뛰기 (중복 방지)
            if (subcategories.contains(category.getC1Name())) {
                log.debug("중복된 C1 카테고리 건너뛰기: {}", category.getC1Name());
                continue;
            }
            subcategories.add(category.getC1Name());
            log.debug("C1 카테고리 추가됨: {}", category.getC1Name());
        }
        log.info("레벨 0 처리 완료. 반환할 최상위 카테고리 개수: {}, 목록: {}", subcategories.size(), subcategories);
        log.info("=== findSubCategoryName 종료 ===");
        return subcategories;
    }

    // ✅ 지원하는 테마 목록 반환 (컨트롤러에서 사용)
    public List<String> getSupportedThemes() {
        // ThemeService에서 테마 목록을 가져오거나, 하드코딩으로 반환
        return List.of("관광지", "문화시설", "축제공연행사", "여행코스", "레포츠", "숙박", "쇼핑", "음식점");
    }

	/**
	 * 데이터 리스트를 DTO로 변환하는 헬퍼 메서드 (대폭 간소화)
	 * @param dataList TODO
	 */
	public List<DataResponseDto> convertToDataResponseDto(List<DataEntity> dataList) {
	    return dataList.stream()
	        .map(DataResponseDto::fromEntity)
	        .collect(Collectors.toList());
	}
}