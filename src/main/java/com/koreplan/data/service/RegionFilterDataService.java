package com.koreplan.data.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;
import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.area.repository.WardCodeRepository;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RegionFilterDataService {

	private final RegionCodeRepository regionCodeRepository;
	private final WardCodeRepository wardCodeRepository;
	private final DataRepository dataRepository;

	/**
	 * 공통 메서드: 지역명으로 RegionCodeEntity 조회
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
	 * 시,도 기준의 데이터 조회 기능
	 */
	@Transactional(readOnly = true)
	public List<DataEntity> findByRegion(String region) {
		log.info("=== findByRegion 시작 - 지역: {} ===", region);

		List<DataEntity> results = new ArrayList<>();

		// 지역 Entity 조회
		RegionCodeEntity regionEntity = getRegionEntity(region);
		if (regionEntity == null) {
			return results;
		}

		log.info("조회된 지역 정보: 지역명={}, 지역코드={}", regionEntity.getName(), regionEntity.getRegioncode());

		// 해당 지역의 데이터 조회
		results = dataRepository.findByRegionCodeEntity(regionEntity);
		// 연관 엔티티 미리 로딩 (LazyInitializationException 방지)
		for (DataEntity data : results) {
			if (data.getWardCodeEntity() != null) {
				data.getWardCodeEntity().getName(); // 강제 로딩
			}
			if (data.getRegionCodeEntity() != null) {
				data.getRegionCodeEntity().getName(); // 강제 로딩
			}
		}
		log.info("조회된 데이터 개수: {}", results.size());
		log.info("=== findByRegion 종료 ===");

		return results;
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

	/**
	 * 기존 방식: Stream 사용 (참고용으로 남겨둠) 여러 단계의 조회가 필요해서 성능상 불리함
	 */
	@Transactional(readOnly = true)
	public List<DataEntity> findByWardLegacy(String regionName, String wardName) {
		log.info("=== findByWardLegacy 시작 - 지역: {}, 구/군: {} ===", regionName, wardName);

		List<DataEntity> results = new ArrayList<>();

		// 입력값 검증
		if (wardName == null || wardName.trim().isEmpty()) {
			log.warn("구/군명이 비어있습니다");
			return results;
		}

		// 지역 Entity 조회
		RegionCodeEntity regionEntity = getRegionEntity(regionName);
		if (regionEntity == null) {
			return results;
		}

		// Stream을 사용해서 구/군 찾기 (성능상 불리)
		Optional<WardCodeEntity> wardEntityOpt = regionEntity.getWardList().stream()
				.filter(ward -> ward.getName().equals(wardName.trim())).findFirst();

		if (wardEntityOpt.isEmpty()) {
			log.warn("지역 '{}'에서 구/군 '{}'를 찾을 수 없습니다", regionName, wardName);
			return results;
		}

		WardCodeEntity wardEntity = wardEntityOpt.get();
		log.info("조회된 구/군 정보: 구/군명={}, 구/군코드={}", wardEntity.getName(), wardEntity.getWardcode());

		// 해당 구/군의 데이터 조회
		results = dataRepository.findByWardCodeEntity(wardEntity);

		log.info("조회된 데이터 개수: {}", results.size());
		log.info("=== findByWardLegacy 종료 ===");

		return results;
	}

	/**
	 * 성능 개선 버전: Repository 직접 조회 한 번의 쿼리로 구/군을 바로 찾아서 성능이 더 좋음
	 */
	@Transactional(readOnly = true)
	public List<DataEntity> findByWard(String regionName, String wardName) {
		log.info("=== findByWard 시작 - 지역: {}, 구/군: {} ===", regionName, wardName);

		List<DataEntity> results = new ArrayList<>();

		// 입력값 검증
		if (regionName == null || regionName.trim().isEmpty() || wardName == null || wardName.trim().isEmpty()) {
			log.warn("지역명 또는 구/군명이 비어있습니다");
			return results;
		}

		// Repository에서 직접 조회 - 한 번의 쿼리로 해결!
		Optional<WardCodeEntity> wardEntityOpt = wardCodeRepository.findByRegionNameAndWardName(regionName.trim(),
				wardName.trim());

		if (wardEntityOpt.isEmpty()) {
			log.warn("지역 '{}'에서 구/군 '{}'를 찾을 수 없습니다", regionName, wardName);
			return results;
		}

		WardCodeEntity wardEntity = wardEntityOpt.get();
		log.info("조회된 구/군 정보: 구/군명={}, 구/군코드={}", wardEntity.getName(), wardEntity.getWardcode());

		// 해당 구/군의 데이터 조회
		results = dataRepository.findByWardCodeEntity(wardEntity);

		log.info("조회된 데이터 개수: {}", results.size());
		log.info("=== findByWard 종료 ===");

		return results;
	}

	/**
	 * 테스트용 메서드
	 */
	private void testFindByRegion(String regionName) {
		log.info("--- 지역 데이터 조회 테스트: {} ---", regionName);
		List<DataEntity> result = findByRegion(regionName);
		log.info("결과: {}개 데이터 조회됨\n", result.size());
	}

	private void testFindWard(String regionName) {
		log.info("--- 구/군 목록 조회 테스트: {} ---", regionName);
		List<String> wards = findWard(regionName);
		log.info("결과: {}개 구/군 - {}\n", wards.size(), wards);
	}

	private void testFindByWard(String regionName, String wardName) {
		log.info("--- 구/군별 데이터 조회 테스트: {} - {} ---", regionName, wardName);
		List<DataEntity> result = findByWard(regionName, wardName);
		log.info("결과: {}개 데이터 조회됨\n", result.size());
	}

	/**
	 * 애플리케이션 시작 후 테스트 실행
	 */
//    @EventListener(ApplicationReadyEvent.class)
//    @Transactional(readOnly = true)
//    public void init() {
//        log.info("=== 애플리케이션 초기화 테스트 시작 ===");
//        
//        try {
//            // 1. 지역별 데이터 조회 테스트
//            testFindByRegion("서울특별시");
//            testFindByRegion("부산광역시");
//            
//            // 2. 구/군 목록 조회 테스트
//            testFindWard("서울특별시");
//            testFindWard("부산광역시");
//            
//            // 3. 구/군별 데이터 조회 테스트
//            testFindByWard("서울특별시", "종로구");
//            testFindByWard("서울특별시", "강남구");
//            testFindByWard("부산광역시", "해운대구");
//            
//            // 4. 예외 케이스 테스트
//            testFindByWard("존재하지않는지역", "테스트구");
//            testFindByWard("서울특별시", "존재하지않는구");
//            
//        } catch (Exception e) {
//            log.error("테스트 중 오류 발생: ", e);
//        }
//        
//        log.info("=== 애플리케이션 초기화 테스트 완료 ===");
//    }
}
