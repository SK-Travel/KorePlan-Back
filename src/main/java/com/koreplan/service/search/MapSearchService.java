package com.koreplan.service.search;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.dto.festival.FestivalResponseDto;
import com.koreplan.dto.search.DataResponseDto;
import com.koreplan.entity.festival.FestivalEntity;
import com.koreplan.repository.festival.FestivalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MapSearchService {
	@Autowired
	private DataRepository dataRepository;
	@Autowired
	private FestivalRepository festivalRepository;
	
	// 기존 메서드 수정 - 일반 장소만 처리 (theme != 15)
	public Page<DataResponseDto> getNearbyPlacesByTheme(
	        double lat, double lng, int theme, int radius, Pageable pageable) {
	    
	    // 축제는 별도 메서드에서 처리
	    if (theme == 15) {
	        throw new IllegalArgumentException("축제 검색은 getNearbyFestivalsByTheme 메서드를 사용하세요.");
	    }
	    
	    // 1. 해당 테마의 모든 데이터 가져오기
	    List<DataEntity> allThemeData = dataRepository.findByTheme(theme);
	    
	    // 2. 반경 내 데이터만 필터링
	    List<DataEntity> nearbyData = allThemeData.stream()
	            .filter(data -> {
	                double distance = calculateDistance(lat, lng,
	                        Double.parseDouble(data.getMapy()),
	                        Double.parseDouble(data.getMapx()));
	                return distance <= radius; // 반경 내만 필터링
	            })
	            .collect(Collectors.toList());
	    
	    // 3. 수동 페이징 처리
	    int start = (int) pageable.getOffset();
	    int end = Math.min((start + pageable.getPageSize()), nearbyData.size());
	    
	    List<DataEntity> pagedData = nearbyData.subList(start, end);
	    
	    // 4. DTO 변환
	    List<DataResponseDto> dtoList = pagedData.stream()
	            .map(DataResponseDto::fromEntity)
	            .collect(Collectors.toList());
	    
	    // 5. Page 객체 생성
	    return new PageImpl<>(dtoList, pageable, nearbyData.size());
	}

	// 새로운 메서드 추가 - 축제 전용 처리
	public Page<FestivalResponseDto> getNearbyFestivalsByTheme(
	        double lat, double lng, int radius, Pageable pageable) {
	    
	    // 1. 현재 진행중이거나 예정인 축제 데이터 가져오기
	    List<FestivalEntity> allFestivalData = festivalRepository.findCurrentAndUpcomingFestivals();
	    
	    // 2. 반경 내 데이터만 필터링
	    List<FestivalEntity> nearbyData = allFestivalData.stream()
	            .filter(data -> {
	                double distance = calculateDistance(lat, lng,
	                        Double.parseDouble(data.getMapy()),
	                        Double.parseDouble(data.getMapx()));
	                return distance <= radius; // 반경 내만 필터링
	            })
	            .collect(Collectors.toList());
	    
	    // 3. 수동 페이징 처리
	    int start = (int) pageable.getOffset();
	    int end = Math.min((start + pageable.getPageSize()), nearbyData.size());
	    
	    List<FestivalEntity> pagedData = nearbyData.subList(start, end);
	    
	    // 4. DTO 변환 - FestivalResponseDto 사용
	    List<FestivalResponseDto> dtoList = pagedData.stream()
	            .map(FestivalResponseDto::from)
	            .collect(Collectors.toList());
	    
	    // 5. Page 객체 생성
	    return new PageImpl<>(dtoList, pageable, nearbyData.size());
	}
	// 거리 계산 메서드 (Haversine 공식)
	private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
	    final int EARTH_RADIUS = 6371; // 지구 반지름 (km)
	    
	    double latDistance = Math.toRadians(lat2 - lat1);
	    double lngDistance = Math.toRadians(lng2 - lng1);
	    
	    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
	        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
	        * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
	    
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    
	    return EARTH_RADIUS * c * 1000; // 미터 단위로 반환
	}
}
