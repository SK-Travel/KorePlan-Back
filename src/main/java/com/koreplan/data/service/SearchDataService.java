package com.koreplan.data.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.data.dto.DataStatsDto;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.entity.theme.ThemeEntity;
import com.koreplan.repository.like.LikeRepository; // 추후 생성
// import com.koreplan.repository.review.ReviewRepository; // 추후 생성
import com.koreplan.repository.review.ReviewRepository;
import com.koreplan.repository.theme.ThemeRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
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

	@Autowired
	private LikeRepository likeRepository;
	@Autowired
	private ReviewRepository reviewRepository;

	/**
	 * 데이터 통계 조회 (ID로)
	 */
	public DataStatsDto getDataStats(Long dataId) {
		log.info("데이터 통계 조회 요청 - dataId: {}", dataId);

		DataEntity data = dataRepository.findById(dataId)
				.orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + dataId));

		return buildStatsDto(data);
	}

	/**
	 * 데이터 통계 조회 (contentId로)
	 */
	public DataStatsDto getDataStatsByContentId(String contentId) {
		log.info("데이터 통계 조회 요청 - contentId: {}", contentId);

		DataEntity data = dataRepository.findByContentId(contentId)
				.orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + contentId));

		return buildStatsDto(data);
	}

	/**
	 * 여러 contentId에 대한 통계 일괄 조회 (새로 추가)
	 */
	public Map<String, DataStatsDto> getBatchDataStatsByContentIds(List<String> contentIds) {
		log.info("배치 데이터 통계 조회 요청 - contentIds 개수: {}", contentIds.size());

		List<DataEntity> dataEntities = dataRepository.findByContentIdIn(contentIds);

		return dataEntities.stream().collect(Collectors.toMap(DataEntity::getContentId, this::buildStatsDto));
	}

	/**
	 * DataEntity로부터 DataStatsDto 생성
	 */
	private DataStatsDto buildStatsDto(DataEntity data) {
		DataStatsDto stats = new DataStatsDto();

		// 1. viewCount는 이미 DataEntity에 저장됨
		stats.setViewCount(data.getViewCount());

		// 2. likeCount - Like 엔터티 개수
		int likeCount = likeRepository.countByDataId(data.getId());
		stats.setLikeCount(likeCount);

		stats.setReviewCount(data.getReviewCount());
		stats.setRating(data.getRating());
		return stats;
	}

	// AI 필터링 Data의 Theme -> Theme의 ContentTypeId -> Theme의 name가져오는 로직
	public String getThemeNameAndByContentTypeId(String contentTypeId) {

		DataEntity data = dataRepository.findByContentId(contentTypeId).orElseThrow(() -> new RuntimeException());

		int themeCode = data.getTheme();

		return themeRepository.findByContentTypeId(themeCode).map(ThemeEntity::getThemeName).orElse("알 수 없음");
	}

}