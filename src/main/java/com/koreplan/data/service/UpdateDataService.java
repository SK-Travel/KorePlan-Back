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
    
    // 조회수 증가
    public void incrementViewCount(Long dataId) {
        dataRepository.incrementViewCount(dataId);
    }

    public void incrementViewCountByContentId(String contentId) {
        log.info("조회수 증가 처리 - contentId: {}", contentId);
        
        DataEntity data = dataRepository.findByContentId(contentId)
            .orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + contentId));
        
        dataRepository.incrementViewCount(data.getId());
        
        log.info("조회수 증가 완료 - contentId: {}, dataId: {}", contentId, data.getId());
    }
}
