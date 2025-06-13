package com.koreplan.service.festival;

import org.springframework.stereotype.Service;

import com.koreplan.entity.festival.FestivalEntity;
import com.koreplan.repository.festival.FestivalRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateFestivalService {
    private final FestivalRepository festivalRepository;

    @Transactional
    public void increaseViewCount(String contentId) {
        log.info("조회수 증가 처리 시작 - contentId: {}", contentId);
        
        // 축제 존재 여부 확인
        if (!festivalRepository.existsById(contentId)) {
            log.error("축제를 찾을 수 없습니다 - contentId: {}", contentId);
            throw new EntityNotFoundException("축제를 찾을 수 없습니다: " + contentId);
        }
        
        // 조회수 증가 실행
        festivalRepository.incrementViewCount(contentId);
        
        log.info("조회수 증가 완료 - contentId: {}", contentId);
    }
}