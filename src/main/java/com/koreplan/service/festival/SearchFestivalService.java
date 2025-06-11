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
            List<FestivalEntity> results = festivalRepository.findByC2Code(cat);
            
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
            List<FestivalEntity> results = festivalRepository.findByRegionCodeEntity_Regioncode(region);
            
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
        
        // 둘 다 비어있으면 전체 조회
        if (region == null || region.isBlank() || c2Name == null || c2Name.isBlank()) {
            results = festivalRepository.findAll();
        }
        // 카테고리만 비어있는 경우
        else if (c2Name.isBlank()) {
            results = getFestivalByRegion(region);
        }
        // 지역만 비어있는 경우
        else if (region.isBlank()) {
            results = getFestivalByC2Code(c2Name);
        }
        // 둘 다 있는 경우
        else {
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
            if (festival.getRegionCodeEntity() != null) {
                festival.getRegionCodeEntity().getName();
            }
            if (festival.getWardCodeEntity() != null) {
                festival.getWardCodeEntity().getName();
            }
        }
        
        return results;
    }

    /**
     * 4-1. 진행예정인 축제 (시작일이 미래)
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalAfter(List<FestivalEntity> pre) {
        List<FestivalEntity> results = new ArrayList<>();
        for (FestivalEntity fes : pre) {
            if (fes.getEventStartDate().isAfter(LocalDate.now())) {
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
        LocalDate today = LocalDate.now();
        
        for (FestivalEntity fes : pre) {
            LocalDate startDate = fes.getEventStartDate();
            LocalDate endDate = fes.getEventEndDate();
            
            if ((startDate.isBefore(today) || startDate.isEqual(today)) && 
                (endDate.isAfter(today) || endDate.isEqual(today))) {
                results.add(fes);
            }
        }
        return results;
    }

    /**
     * 5. 특정 날짜에 진행중인 축제 가져오기
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalByDate(LocalDate date) {
        List<FestivalEntity> allFestivals = festivalRepository.findAll();
        List<FestivalEntity> results = new ArrayList<>();
        
        for (FestivalEntity fes : allFestivals) {
            LocalDate start = fes.getEventStartDate();
            LocalDate end = fes.getEventEndDate();
            
            // 매개변수로 받은 date가 start와 end 사이에 존재하는지 확인
            if ((start.isBefore(date) || start.isEqual(date)) && 
                (end.isAfter(date) || end.isEqual(date))) {
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

    /**
     * 6. 특정 월에 해당하는 축제 가져오기 (2025년 한정)
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalByMonth(int month) {
        int currentYear = 2025; // 고정값
        YearMonth yearMonth = YearMonth.of(currentYear, month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();
        
        List<FestivalEntity> allFestivals = festivalRepository.findAll();
        List<FestivalEntity> results = new ArrayList<>();
        
        for (FestivalEntity fes : allFestivals) {
            LocalDate festivalStart = fes.getEventStartDate();
            LocalDate festivalEnd = fes.getEventEndDate();
            
            // 축제 기간이 해당 월과 겹치는지 확인
            if (!(festivalEnd.isBefore(startOfMonth) || festivalStart.isAfter(endOfMonth))) {
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

    /**
     * 현재 월의 축제 가져오기 (자동으로 현재 년도/월 사용)
     */
    @Transactional(readOnly = true)
    public List<FestivalEntity> getFestivalThisMonth() {
        LocalDate today = LocalDate.now();
        return getFestivalByMonth(today.getMonthValue());
    }

    /**
     * 진행 상태 확인 유틸리티 메서드들
     */
    
    // 현재 진행중인지 여부 확인
    public boolean getIsOngoing(FestivalEntity festival) {
        LocalDate today = LocalDate.now();
        LocalDate start = festival.getEventStartDate();
        LocalDate end = festival.getEventEndDate();
        
        return (start.isBefore(today) || start.isEqual(today)) && 
               (end.isAfter(today) || end.isEqual(today));
    }

    // 진행 예정인지 여부 확인
    public boolean getIsUpcoming(FestivalEntity festival) {
        return festival.getEventStartDate().isAfter(LocalDate.now());
    }

    // 종료되었는지 여부 확인
    public boolean getIsEnded(FestivalEntity festival) {
        return festival.getEventEndDate().isBefore(LocalDate.now());
    }

    // 축제 상태 문자열로 반환
    public String getFestivalStatusString(FestivalEntity festival) {
        if (getIsUpcoming(festival)) {
            return "진행예정";
        } else if (getIsOngoing(festival)) {
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
            .limit(5)
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
    
    // 커스텀 개수로 인기 축제 조회
    @Transactional(readOnly = true)
    public List<FestivalEntity> getPopularFestivals(int limit) {
        List<FestivalEntity> allFestivals = festivalRepository.findAll();
        List<FestivalEntity> results = allFestivals.stream()
            .sorted((f1, f2) -> Integer.compare(f2.getViewCount(), f1.getViewCount()))
            .limit(limit)
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

    // 지역과 카테고리로 현재 진행 중인 축제만 조회
    @Transactional(readOnly = true)
    public List<FestivalEntity> getOngoingFestivalsByFilter(String regionName, String c2Name) {
        List<FestivalEntity> filtered = getFestivalByTwoOpt(regionName, c2Name);
        return getFestivalGoing(filtered);
    }

    // 지역과 카테고리로 진행 예정 축제만 조회
    @Transactional(readOnly = true)
    public List<FestivalEntity> getUpcomingFestivalsByFilter(String regionName, String c2Name) {
        List<FestivalEntity> filtered = getFestivalByTwoOpt(regionName, c2Name);
        return getFestivalAfter(filtered);
    }
}