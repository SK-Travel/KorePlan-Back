package com.koreplan.service.festival;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.category.repository.CategoryRepository;
import com.koreplan.entity.festival.FestivalEntity;
import com.koreplan.repository.festival.FestivalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // 클래스 레벨에 추가
public class SearchFestivalService {
    private final FestivalRepository festivalRepository;
    private final CategoryRepository categoryRepository;
    private final RegionCodeRepository regionCodeRepository;

    /**
     * 1. 축제/공연/행사(C2Code)별로 축제 가져오는 메서드
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalByC2Code(String c2Name) {
        try {
            String cat = categoryRepository.findByC2Name(c2Name).getFirst().getC2Code();
            List<FestivalEntity> results = festivalRepository.findByC2CodeOrderByViewCountDesc(cat);
            
            // 연관 엔티티 미리 로딩 (LazyInitializationException 방지)
            for (FestivalEntity festival : results) {
                if (festival.getRegionCodeEntity() != null) {
                    festival.getRegionCodeEntity().getName(); // 강제 로딩
                }
                if (festival.getWardCodeEntity() != null) {
                    festival.getWardCodeEntity().getName(); // 강제 로딩
                }
            }
            
            return results;
        } catch (Exception e) {
            log.error("C2Code로 축제 조회 실패: {}", c2Name, e);
            return new ArrayList<>();
        }
    }

    /**
     * 2. 지역별 축제 가져오는 메서드
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalByRegion(String regionName) {
        try {
            Long region = regionCodeRepository.findByName(regionName).getRegioncode();
            List<FestivalEntity> results = festivalRepository.findByRegionCodeEntity_RegioncodeOrderByViewCountDesc(region);
            
            // 연관 엔티티 미리 로딩
            for (FestivalEntity festival : results) {
                if (festival.getRegionCodeEntity() != null) {
                    festival.getRegionCodeEntity().getName();
                }
                if (festival.getWardCodeEntity() != null) {
                    festival.getWardCodeEntity().getName();
                }
            }
            
            return results;
        } catch (Exception e) {
            log.error("지역별 축제 조회 실패: {}", regionName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 3. 지역별, C2코드별로 이중 필터링 메서드
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalByTwoOpt(String region, String c2Name) {
        List<FestivalEntity> results;
        
        // null 안전 체크
        boolean regionEmpty = (region == null || region.isBlank());
        boolean categoryEmpty = (c2Name == null || c2Name.isBlank());
        
        log.info("🔧 getFestivalByTwoOpt: region='{}' (empty={}), c2Name='{}' (empty={})", 
            region, regionEmpty, c2Name, categoryEmpty);
        
        // 둘 다 비어있으면 전체 조회
        if (regionEmpty && categoryEmpty) {
            log.info("📊 전체 축제 조회");
            results = festivalRepository.findAllByOrderByViewCountDesc();
        }
        // 카테고리만 비어있는 경우 (지역만 있음)
        else if (categoryEmpty && !regionEmpty) {
            log.info("🗺️ 지역별 조회: {}", region);
            results = getFestivalByRegion(region);
        }
        // 지역만 비어있는 경우 (카테고리만 있음)
        else if (regionEmpty && !categoryEmpty) {
            log.info("🎭 카테고리별 조회: {}", c2Name);
            results = getFestivalByC2Code(c2Name);
        }
        // 둘 다 있는 경우
        else {
            log.info("🎯 지역+카테고리 조회: region={}, category={}", region, c2Name);
            try {
                results = new ArrayList<>(getFestivalByRegion(region));
                String cat = categoryRepository.findByC2Name(c2Name).getFirst().getC2Code();
                
                // Iterator를 사용하여 안전하게 제거
                Iterator<FestivalEntity> iterator = results.iterator();
                while (iterator.hasNext()) {
                    FestivalEntity fes = iterator.next();
                    if (!fes.getC2Code().equals(cat)) {
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                log.error("이중 필터링 실패: region={}, c2Name={}", region, c2Name, e);
                results = new ArrayList<>();
            }
        }
        
        // 연관 엔티티 미리 로딩
        for (FestivalEntity festival : results) {
            try {
                if (festival.getRegionCodeEntity() != null) {
                    festival.getRegionCodeEntity().getName();
                }
                if (festival.getWardCodeEntity() != null) {
                    festival.getWardCodeEntity().getName();
                }
            } catch (Exception e) {
                log.warn("연관 엔티티 로딩 실패: contentId={}", festival.getContentId(), e);
            }
        }
        
        log.info("✅ getFestivalByTwoOpt 결과: {}개", results.size());
        return results;
    }

    /**
     * 4-1. 진행예정인 축제 (시작일이 미래)
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalAfter(List<FestivalEntity> pre) {
        List<FestivalEntity> results = new ArrayList<>();
        for (FestivalEntity fes : pre) {
            if (fes.isUpcoming()) {
                results.add(fes);
            }
        }
        return results;
    }

    /**
     * 4-2. 진행중인 축제 (시작일 ≤ 오늘 ≤ 종료일)
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalGoing(List<FestivalEntity> pre) {
        List<FestivalEntity> results = new ArrayList<>();
        for (FestivalEntity fes : pre) {
            if(fes.isOngoing()) {
            	results.add(fes);
            }
        }
        return results;
    }

    

    /**
     * 6. 특정 월에 해당하는 축제 가져오기 (2025년 한정)
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalByMonth(int month , List<FestivalEntity> pre) {
        //List<FestivalEntity> allFestivals = festivalRepository.findAll();
        
        List<FestivalEntity> results = pre.stream()
            .filter(festival -> festival.isRunningInMonth(month))
            .toList();
        
        // 연관 엔티티 미리 로딩
        for (FestivalEntity festival : results) {
            if (festival.getRegionCodeEntity() != null) {
                festival.getRegionCodeEntity().getName();
            }
            if (festival.getWardCodeEntity() != null) {
                festival.getWardCodeEntity().getName();
            }
        }
        
        return results;
    }
    //총합 필터링 모든 경우의 수 필터링 가능
    public List<FestivalEntity> getComplexFilteredFestivals(String regionName, String c2Name, String status, Integer month) {
        List<FestivalEntity> filtered = getFestivalByTwoOpt(regionName, c2Name);
        
        if ("진행중".equals(status)) {
            return getFestivalGoing(filtered);      // 기존 로직과 동일
        } else if ("진행예정".equals(status)) {
            return getFestivalAfter(filtered);      // 기존 로직과 동일
        } else if (month != null) {
            return getFestivalByMonth(month, filtered);
        }
        
        return filtered;
    }
    
 

    // 축제 상태 문자열로 반환
    public String getFestivalStatusString(FestivalEntity festival) {
        if (festival.isUpcoming()) {
            return "진행예정";
        } else if (festival.isOngoing()) {
            return "진행중";
        } else {
            return "종료됨";
        }
    }

    /**
     * 추가 유틸리티 메서드들
     */
    
    // 인기 축제 조회 (조회수 기준 상위 5개)
    @Transactional(readOnly = true)
    public List<FestivalEntity> getPopularFestivals() {
        List<FestivalEntity> allFestivals = festivalRepository.findAll();
        List<FestivalEntity> results = allFestivals.stream()
            .sorted((f1, f2) -> Integer.compare(f2.getViewCount(), f1.getViewCount()))
            .limit(5)//5개에 조정가능
            .toList();
            
        // 연관 엔티티 미리 로딩
        for (FestivalEntity festival : results) {
            if (festival.getRegionCodeEntity() != null) {
                festival.getRegionCodeEntity().getName();
            }
            if (festival.getWardCodeEntity() != null) {
                festival.getWardCodeEntity().getName();
            }
        }
        
        return results;
    }
    
    
    
    
    // 키워드로 축제 검색 (제목 기준)
    @Transactional(readOnly = true)
    public List<FestivalEntity> searchFestivalsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<FestivalEntity> allFestivals = festivalRepository.findAll();
        List<FestivalEntity> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (FestivalEntity fes : allFestivals) {
            if (fes.getTitle() != null && 
                fes.getTitle().toLowerCase().contains(lowerKeyword)) {
                results.add(fes);
            }
        }
        
        // 연관 엔티티 미리 로딩
        for (FestivalEntity festival : results) {
            if (festival.getRegionCodeEntity() != null) {
                festival.getRegionCodeEntity().getName();
            }
            if (festival.getWardCodeEntity() != null) {
                festival.getWardCodeEntity().getName();
            }
        }
        
        return results;
    }
}