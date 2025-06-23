package com.koreplan.data.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.dto.list.DataSearchDto;
import com.koreplan.dto.search.DataResponseDto;
import com.koreplan.entity.theme.ThemeEntity;
import com.koreplan.repository.theme.ThemeRepository;
import com.koreplan.service.search.FilterDataService.SortType;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SearchDataService {

	@Autowired
	private DataRepository dataRepository;

	@Autowired
	private ThemeRepository themeRepository;

	@Transactional(readOnly = true)
	public List<DataResponseDto> getAsDto(List<DataEntity> entities) {
		log.info("점수 기준 상위 5개 데이터 조회 (DTO 변환 포함)");

	
		// 트랜잭션 내에서 DTO 변환 (지연 로딩 가능)
		return entities.stream().map(DataResponseDto::fromEntity).collect(Collectors.toList());
	}

	/**
	 * 데이터 조회 (ID로) - 모든 통계 정보 포함
	 */
	public DataEntity getDataWithStats(Long dataId) {
		log.info("데이터 조회 요청 - dataId: {}", dataId);

		return dataRepository.findById(dataId)
				.orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + dataId));
	}

	/**
	 * 데이터 조회 (contentId로) - 모든 통계 정보 포함
	 */
	@Transactional(readOnly = true)
	public DataEntity getDataWithStatsByContentId(String contentId) {
		log.info("데이터 조회 요청 - contentId: {}", contentId);

		return dataRepository.findByContentId(contentId)
				.orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + contentId));
	}
	@Transactional(readOnly = true)
	public DataResponseDto getDataResponseDtoByContentId(String contentId) {
	    DataEntity entity = getDataWithStatsByContentId(contentId);
	    if (entity == null) {
	        return null;
	    }
	    return DataResponseDto.fromEntity(entity);  // 트랜잭션 안에서 DTO 변환
	}

	/**
	 * 여러 contentId에 대한 데이터 일괄 조회
	 */
	//zz
	public Map<String, DataEntity> getBatchDataByContentIds(List<String> contentIds) {
		log.info("배치 데이터 조회 요청 - contentIds 개수: {}", contentIds.size());

		List<DataEntity> dataEntities = dataRepository.findByContentIdIn(contentIds);

		return dataEntities.stream().collect(Collectors.toMap(DataEntity::getContentId, data -> data));
	}

	/**
	 * 점수 기준 상위 5개 관광지 데이터 조회 (숙박 제외)
	 */
	@Transactional(readOnly = true)
	public List<DataEntity> getTop5PlacesByScore() {
	    log.info("점수 기준 상위 5개 관광지 데이터 조회 (숙박 제외)");
	    
	    return dataRepository.findTop5ByOrderByScoreDescExcludingAccommodation();
	}

	/**
	 * 점수 기준 상위 5개 숙박 데이터 조회
	 */
	@Transactional(readOnly = true)
	public List<DataEntity> getTop5HotelsByScore() {
	    log.info("점수 기준 상위 5개 숙박 데이터 조회");
	    
	    return dataRepository.findTop5ByC1CodeOrderByScoreDesc();
	}

	/**
	 * 통합 검색 기능 - title, region name, ward name 포함 검색
	 * 
	 * @param keyword 검색 키워드
	 * @return 검색된 데이터 리스트 (기본: 점수 순 정렬)
	 */
	public List<DataEntity> searchByKeyword(String keyword) {
		return searchByKeyword(keyword, SortType.SCORE);
	}

	/**
	 * 통합 검색 기능 (정렬 포함) - title, region name, ward name 포함 검색
	 * 
	 * @param keyword  검색 키워드
	 * @param sortType 정렬 타입
	 * @return 검색된 데이터 리스트
	 */
	public List<DataEntity> searchByKeyword(String keyword, SortType sortType) {
		log.info("통합 검색 시작 - 키워드: '{}', 정렬: {}", keyword, sortType);

		if (keyword == null || keyword.trim().isEmpty()) {
			log.warn("검색 키워드가 비어있습니다.");
			return List.of();
		}

		String trimmedKeyword = keyword.trim();

		// 1. 전체 데이터를 정렬된 상태로 가져오기
		List<DataEntity> allData = getAllDataSortedBy(sortType);

		// 2. 키워드로 필터링
		List<DataEntity> searchResults = allData.stream().filter(data -> matchesKeyword(data, trimmedKeyword))
				.collect(Collectors.toList());

		log.info("검색 완료 - 키워드: '{}', 결과: {}개", trimmedKeyword, searchResults.size());
		return searchResults;
	}

	/**
	 * 정렬 타입에 따라 전체 데이터를 정렬된 상태로 가져오는 헬퍼 메서드
	 */
	private List<DataEntity> getAllDataSortedBy(SortType sortType) {
		return switch (sortType) {
		case SCORE -> dataRepository.findAllByOrderByScoreDesc();
		case VIEW_COUNT -> dataRepository.findAllByOrderByViewCountDesc();
		case LIKE_COUNT -> dataRepository.findAllByOrderByLikeCountDesc();
		case RATING -> dataRepository.findAllByOrderByRatingDesc();
		case REVIEW_COUNT -> dataRepository.findAllByOrderByReviewCountDesc();
		};
	}

	/**
	 * 데이터가 키워드와 매칭되는지 확인하는 헬퍼 메서드 - title 포함 검색 - region name 포함 검색 - ward name 포함
	 * 검색
	 */
	private boolean matchesKeyword(DataEntity data, String keyword) {
		String lowerKeyword = keyword.toLowerCase();

		// 1. Title 검색
		if (data.getTitle() != null && data.getTitle().toLowerCase().contains(lowerKeyword)) {
			return true;
		}

		// 2. Region Name 검색
		if (data.getRegionCodeEntity() != null && data.getRegionCodeEntity().getName() != null
				&& data.getRegionCodeEntity().getName().toLowerCase().contains(lowerKeyword)) {
			return true;
		}

		// 3. Ward Name 검색
		if (data.getWardCodeEntity() != null && data.getWardCodeEntity().getName() != null
				&& data.getWardCodeEntity().getName().toLowerCase().contains(lowerKeyword)) {
			return true;
		}

		return false;
	}

	/**
	 * 페이징을 지원하는 통합 검색 기능
	 * 
	 * @param keyword  검색 키워드
	 * @param sortType 정렬 타입
	 * @param pageable 페이징 정보
	 * @return 페이징된 검색 결과
	 */
	public Page<DataEntity> searchByKeywordWithPaging(String keyword, SortType sortType, Pageable pageable) {
		log.info("페이징 통합 검색 시작 - 키워드: '{}', 정렬: {}, 페이지: {}", keyword, sortType, pageable.getPageNumber());

		// 먼저 검색 결과를 모두 가져온 후 수동으로 페이징 처리
		List<DataEntity> searchResults = searchByKeyword(keyword, sortType);

		// 페이징 처리
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), searchResults.size());

		List<DataEntity> pagedResults = searchResults.subList(start, end);

		log.info("페이징 검색 완료 - 전체: {}개, 페이지 결과: {}개", searchResults.size(), pagedResults.size());

		return new PageImpl<>(pagedResults, pageable, searchResults.size());
	}

	/**
	 * AI 필터링 Data의 Theme -> Theme의 ContentTypeId -> Theme의 name 가져오는 로직
	 */
	public String getThemeNameByContentTypeId(String contentTypeId) {
		log.info("테마명 조회 요청 - contentTypeId: {}", contentTypeId);

		DataEntity data = dataRepository.findByContentId(contentTypeId)
				.orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + contentTypeId));

		int themeCode = data.getTheme();

		return themeRepository.findByContentTypeId(themeCode).map(ThemeEntity::getThemeName).orElse("알 수 없음");
	}
	
	
	// 리스트 추가 시 검색 로직
	public List<DataSearchDto> searchByKeywordList(String keyword) {
	    if (keyword == null || keyword.trim().isEmpty()) return new ArrayList<>();
	    
	   
	    
	    List<DataEntity> allData = getAllDataSortedBy(SortType.SCORE);
	    
	    List<DataSearchDto> result = new ArrayList<>();
	    for (DataEntity data : allData) {
	        boolean matches = false;
	        if(matchesKeyword(data,keyword)) {
	        	matches = true;
	        	DataSearchDto dto = new DataSearchDto();
	            dto.setId(data.getId());
	            dto.setTitle(data.getTitle());

	            if (data.getRegionCodeEntity() != null) {
	                dto.setRegionName(data.getRegionCodeEntity().getName());
	            }
	            
	            if(data.getFirstimage() != null) {
	            	dto.setFirstimage(data.getFirstimage());
	            }
	            
	            
	            result.add(dto);
	        }
	        
	    }
	    return result;
	}
	
	
}