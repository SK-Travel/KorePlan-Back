package com.koreplan.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpdateDataService {
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private ScoreCalculationService scoreCalculationService; // ✅ 추가
    
    // 조회수 증가 (Score 계산 추가)
    public void incrementViewCount(Long dataId) {
        dataRepository.incrementViewCount(dataId);
        
        // ✅ Score 실시간 업데이트
        scoreCalculationService.updateScore(dataId);
        
        log.info("조회수 및 Score 업데이트 완료 - dataId: {}", dataId);
    }
    
    // contentId로 조회수 증가 (Score 계산 추가)
    public void incrementViewCountByContentId(String contentId) {
        log.info("조회수 증가 처리 - contentId: {}", contentId);
        
        DataEntity data = dataRepository.findByContentId(contentId)
            .orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + contentId));
        
        dataRepository.incrementViewCount(data.getId());
        
        // ✅ Score 실시간 업데이트
        scoreCalculationService.updateScore(data.getId());
        
        log.info("조회수 및 Score 업데이트 완료 - contentId: {}, dataId: {}", contentId, data.getId());
    }
}