package com.koreplan.service.like;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.data.service.ScoreCalculationService; 
import com.koreplan.entity.like.LikeEntity;
import com.koreplan.repository.like.LikeRepository;

@Service
public class LikeService {
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private ScoreCalculationService scoreCalculationService; 
    
    // 기존 좋아요 토글 메서드 (Score 계산 추가)
    @Transactional
    public boolean likeToggle(Long dataId, int userId) {
        boolean exists = likeRepository.existsByDataIdAndUserId(dataId, userId);
        
        if (exists) {
            // 좋아요 삭제 + DataEntity likeCount -1
            likeRepository.deleteByDataIdAndUserId(dataId, userId);
            dataRepository.decrementLikeCount(dataId);
            
           //Score 업데이트
            scoreCalculationService.updateScore(dataId);
            
            return false;
        } else {
            // 좋아요 추가 + DataEntity likeCount +1  
            LikeEntity like = LikeEntity.builder()
                .dataId(dataId).userId(userId).build();
            likeRepository.save(like);
            dataRepository.incrementLikeCount(dataId);
            
            // Score 업데이트
            scoreCalculationService.updateScore(dataId);
            
            return true;
        }
    }
    
    // 사용자가 좋아요한 모든 데이터 ID 조회
    public Set<Long> getUserLikedDataIds(int userId) {
        System.out.println("사용자 " + userId + "의 좋아요 목록 조회 시작");
        List<LikeEntity> likedItems = likeRepository.findByUserId(userId);
        Set<Long> likedDataIds = likedItems.stream()
                .map(LikeEntity::getDataId)
                .collect(Collectors.toSet());
        System.out.println("조회된 좋아요 개수: " + likedDataIds.size());
        return likedDataIds;
    }
    
    // 여러 데이터의 좋아요 상태 확인
    public Map<Long, Boolean> checkLikeStatus(int userId, List<Long> dataIds) {
        System.out.println("사용자 " + userId + "의 좋아요 상태 확인: " + dataIds);
        Map<Long, Boolean> statusMap = new HashMap<>();
        for (Long dataId : dataIds) {
            boolean isLiked = likeRepository.existsByDataIdAndUserId(dataId, userId);
            statusMap.put(dataId, isLiked);
        }
        System.out.println("좋아요 상태 결과: " + statusMap);
        return statusMap;
    }
    
    // 특정 데이터의 좋아요 상태 확인 (단일)
    public boolean isLikedByUser(int userId, Long dataId) {
        return likeRepository.existsByDataIdAndUserId(dataId, userId);
    }
    
    // 특정 데이터의 좋아요 수 조회
    public long getLikeCount(Long dataId) {
        return likeRepository.countByDataId(dataId);
    }
    
    // 사용자의 총 좋아요 수 조회
    public long getUserLikeCount(int userId) {
        return likeRepository.countByUserId(userId);
    }
}