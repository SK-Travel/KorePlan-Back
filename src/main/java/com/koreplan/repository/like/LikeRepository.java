package com.koreplan.repository.like;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreplan.entity.like.LikeEntity;

public interface LikeRepository extends JpaRepository<LikeEntity, Integer> {

    // 기존 메서드: 특정 유저가 특정 데이터(dataId)에 대해 좋아요 했는지 확인
    boolean existsByDataIdAndUserId(Long dataId, int userId);

    // 기존 메서드: 특정 유저의 특정 좋아요 삭제
    void deleteByDataIdAndUserId(Long dataId, int userId);

    // 특정 사용자의 모든 좋아요 조회
    List<LikeEntity> findByUserId(int userId);

    // 특정 데이터의 좋아요 수 조회
    int countByDataId(Long dataId);

    //사용자의 좋아요 총 개수
    long countByUserId(int userId);

}