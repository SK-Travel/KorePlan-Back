package com.koreplan.repository.like;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.entity.like.LikeEntity;

public interface LikeRepository extends JpaRepository <LikeEntity, Integer> {
	

    // 특정 유저가 특정 데이터(dataId)에 대해 좋아요 했는지 확인 ==> 존재 여부
    boolean existsByDataIdAndUserId(Long dataId, int userId);

    // 특정 유저의 특정 좋아요 삭제 ==> 토글 시 삭제
    void deleteByDataIdAndUserId(Long dataId, int userId);
	
	
}
