package com.koreplan.repository.review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.koreplan.entity.review.ReviewEntity;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    // ==================== 기본 조회 메서드 ====================
    
    /**
     * 특정 데이터(장소)의 모든 리뷰 조회 (페이징)
     */
    Page<ReviewEntity> findByDataEntityIdOrderByCreatedAtDesc(Long dataId, Pageable pageable);
    
    /**
     * 특정 데이터(장소)의 모든 리뷰 조회 (리스트)
     */
    List<ReviewEntity> findByDataEntityIdOrderByCreatedAtDesc(Long dataId);
    
    /**
     * 특정 사용자의 모든 리뷰 조회
     */
    List<ReviewEntity> findByUserEntityIdOrderByCreatedAtDesc(int userId);
    List<ReviewEntity> findByUserEntityIdOrderByUpdatedAtDesc(int userId);
    
    /**
     * 사용자가 특정 데이터에 작성한 리뷰 조회 (중복 리뷰 방지용)
     */
    Optional<ReviewEntity> findByUserEntityIdAndDataEntityId(int userId, Long dataId);
    
    /**
     * 특정 데이터의 모든 리뷰 조회 (통계용)
     */
    List<ReviewEntity> findByDataEntityId(Long dataId);

    // ==================== 통계 관련 메서드 ====================
    
    /**
     * 특정 데이터의 리뷰 개수
     */
    int countByDataEntityId(Long dataId);
    
    /**
     * 특정 사용자의 리뷰 개수
     */
    int countByUserEntityId(int userId);
    
    /**
     * 특정 데이터의 평균 평점 조회
     */
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.dataEntity.id = :dataId")
    Double getAverageRatingByDataEntityId(@Param("dataId") Long dataId);
    
    /**
     * 특정 데이터의 평점별 개수 조회
     */
    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r WHERE r.dataEntity.id = :dataId GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingCountsByDataEntityId(@Param("dataId") Long dataId);

    // ==================== 고급 조회 메서드 ====================
    
    /**
     * 특정 평점 이상의 리뷰만 조회
     */
    List<ReviewEntity> findByDataEntityIdAndRatingGreaterThanEqualOrderByCreatedAtDesc(Long dataId, int minRating);
    
    /**
     * 특정 평점의 리뷰만 조회
     */
    List<ReviewEntity> findByDataEntityIdAndRatingOrderByCreatedAtDesc(Long dataId, int rating);
    
    /**
     * 최근 N개월 내 리뷰 조회
     */
    @Query("SELECT r FROM ReviewEntity r WHERE r.dataEntity.id = :dataId AND r.createdAt >= CURRENT_DATE - :months MONTH ORDER BY r.createdAt DESC")
    List<ReviewEntity> findRecentReviewsByDataEntityId(@Param("dataId") Long dataId, @Param("months") int months);
    
    /**
     * 특정 키워드를 포함한 리뷰 검색
     */
    @Query("SELECT r FROM ReviewEntity r WHERE r.dataEntity.id = :dataId AND r.content LIKE %:keyword% ORDER BY r.createdAt DESC")
    List<ReviewEntity> findByDataEntityIdAndContentContaining(@Param("dataId") Long dataId, @Param("keyword") String keyword);

    // ==================== 배치 조회 메서드 ====================
    
    /**
     * 여러 데이터 ID에 대한 리뷰 개수 조회
     */
    @Query("SELECT r.dataEntity.id, COUNT(r) FROM ReviewEntity r WHERE r.dataEntity.id IN :dataIds GROUP BY r.dataEntity.id")
    List<Object[]> getReviewCountsByDataEntityIds(@Param("dataIds") List<Long> dataIds);
    
    /**
     * 여러 데이터 ID에 대한 평균 평점 조회  
     */
    @Query("SELECT r.dataEntity.id, AVG(r.rating) FROM ReviewEntity r WHERE r.dataEntity.id IN :dataIds GROUP BY r.dataEntity.id")
    List<Object[]> getAverageRatingsByDataEntityIds(@Param("dataIds") List<Long> dataIds);

    // ==================== 관리자용 메서드 ====================
    
    /**
     * 전체 리뷰 개수
     */
    long count();
    
    /**
     * 특정 기간 내 작성된 리뷰 조회
     */
    @Query("SELECT r FROM ReviewEntity r WHERE r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<ReviewEntity> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                             @Param("endDate") java.time.LocalDateTime endDate);

}