package com.koreplan.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreCalculationService {
    
    @Autowired
    private DataRepository dataRepository;
    
    /**
     * Score 계산 공식: (조회수 ÷ 10 × 1) + (찜수 × 3) + (리뷰수 × 2) + (신뢰도 보정된 평점 점수)
     */
    public double calculateScore(int viewCount, int likeCount, int reviewCount, double rating) {
        double viewScore = (viewCount / 10.0) * 1.0; // 조회수 ÷ 10 × 1
        double likeScore = likeCount * 3.0; // 찜수 × 3
        double reviewScore = reviewCount * 2.0; // 리뷰수 × 2
        
        // 신뢰도 보정된 평점 점수
        double ratingScore = (rating - 2.5) * 4.0 * getReviewCountWeight(reviewCount);
        
        double totalScore = viewScore + likeScore + reviewScore + ratingScore;
        
        // 소수점 1자리로 반올림
        return Math.round(totalScore * 10.0) / 10.0;
    }

    /**
     * 리뷰 개수에 따른 가중치 계산
     */
    private double getReviewCountWeight(int reviewCount) {
        if (reviewCount >= 100) {
            return 1.0;     // 100개 이상: 가중치 그대로
        } else if (reviewCount >= 50) {
            return 0.8;     // 50-99개: 80%
        } else if (reviewCount >= 20) {
            return 0.6;     // 20-49개: 60%
        } else {
            return 0.4;     // 20개 미만: 40%
        }
    }
    
    /**
     * 특정 데이터의 score 실시간 업데이트
     */
    @Transactional
    public void updateScore(Long dataId) {
        log.info("Score 업데이트 시작 - dataId: {}", dataId);
        
        DataEntity data = dataRepository.findById(dataId)
            .orElseThrow(() -> new EntityNotFoundException("데이터를 찾을 수 없습니다: " + dataId));
        
        // 현재 값들로 score 계산
        double newScore = calculateScore(
            data.getViewCount(),
            data.getLikeCount(), 
            data.getReviewCount(),
            data.getRating()
        );
        
        // score 업데이트
        data.setScore(newScore);
        dataRepository.save(data);
        
        log.info("Score 업데이트 완료 - dataId: {}, 조회수: {}, 찜수: {}, 리뷰수: {}, 평점: {}, 새로운Score: {}", 
            dataId, data.getViewCount(), data.getLikeCount(), data.getReviewCount(), data.getRating(), newScore);
    }
    
    /**
     * Score 미리보기 (실제 저장하지 않음)
     */
    public double previewScore(int viewCount, int likeCount, int reviewCount, double rating) {
        return calculateScore(viewCount, likeCount, reviewCount, rating);
    }
}