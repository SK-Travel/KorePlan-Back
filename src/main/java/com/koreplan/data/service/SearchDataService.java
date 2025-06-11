package com.koreplan.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.data.dto.DataStatsDto;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.entity.theme.ThemeEntity;
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
    
    /**
     * 데이터 통계 조회 (ID로)
     * @param dataId 데이터 ID
     * @return 조회수, 좋아요수, 리뷰수, 평점
     */
    public DataStatsDto getDataStats(Long dataId) {
        log.info("데이터 통계 조회 요청 - dataId: {}", dataId);
        
        DataEntity data = dataRepository.findById(dataId)
            .orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + dataId));
        
        DataStatsDto stats = new DataStatsDto();
        stats.setViewCount(data.getViewCount());
        stats.setLikeCount(0); // TODO: 좋아요 테이블에서 계산
        stats.setReviewCount(0); // TODO: 리뷰 테이블에서 계산
        stats.setRating(0.0); // TODO: 리뷰 테이블에서 계산
            
        log.info("데이터 통계 조회 완료 - dataId: {}, viewCount: {}", dataId, stats.getViewCount());
        return stats;
    }
    
    /**
     * 데이터 통계 조회 (contentId로)
     * @param contentId 콘텐츠 ID
     * @return 데이터 통계
     */
    public DataStatsDto getDataStatsByContentId(String contentId) {
        log.info("데이터 통계 조회 요청 - contentId: {}", contentId);
        
        DataEntity data = dataRepository.findByContentId(contentId)
            .orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + contentId));
        
        return getDataStats(data.getId());
    }
    
    
    // AI 필터링 Data의 Theme -> Theme의 ContentTypeId -> Theme의 name가져오는 로직
    public String getThemeNameAndByContentTypeId(String contentTypeId) {
    	
    	DataEntity data = dataRepository.findByContentId(contentTypeId)
    			.orElseThrow(() -> new RuntimeException());
    	
    	int themeCode = data.getTheme();
    	
    	return themeRepository.findByContentTypeId(themeCode)
    			.map(ThemeEntity::getThemeName)
    			.orElse("알 수 없음");
    }
    
    
}
