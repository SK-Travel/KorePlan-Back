package com.koreplan.service.review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;
import com.koreplan.entity.review.ReviewEntity;
import com.koreplan.repository.review.ReviewRepository;
import com.koreplan.user.entity.UserEntity;
import com.koreplan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
	private final ReviewRepository reviewRepository;
	private final DataRepository dataRepository;
	private final UserRepository userRepository;
	
	
	 /**
     * 리뷰 작성
     */
    public ReviewEntity createReview(Long dataId, int userId, int rating, String content) {
        // 데이터 존재 확인
        DataEntity dataEntity = dataRepository.findById(dataId)
                .orElseThrow(() -> new IllegalArgumentException("해당 데이터를 찾을 수 없습니다: " + dataId));
        
        // 사용자 존재 확인
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다: " + userId));
        
        // 평점 유효성 검사
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5점 사이여야 합니다.");
        }
        
        // 리뷰 생성
        ReviewEntity review = ReviewEntity.builder()
                .dataEntity(dataEntity)
                .userEntity(userEntity)
                .rating(rating)
                .content(content)
                .build();
        
        ReviewEntity savedReview = reviewRepository.save(review);
        
        // 데이터의 평균 평점 업데이트
        updateDataAverageRating(dataId);
        
        return savedReview;
    }

    /**
     * 리뷰 수정
     */
    public ReviewEntity updateReview(Long reviewId, int userId, int rating, String content) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰를 찾을 수 없습니다: " + reviewId));
        
        // 작성자 확인
        if (!(review.getUserEntity().getId()==(userId))) {
            throw new IllegalArgumentException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }
        
        // 평점 유효성 검사
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5점 사이여야 합니다.");
        }
        
        // 리뷰 업데이트
        review.setRating(rating);
        review.setContent(content);
        
        ReviewEntity updatedReview = reviewRepository.save(review);
        
        // 데이터의 평균 평점 업데이트
        updateDataAverageRating(review.getDataEntity().getId());
        
        return updatedReview;
    }

    /**
     * 리뷰 삭제
     */
    public void deleteReview(Long reviewId, int userId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰를 찾을 수 없습니다: " + reviewId));
        
        // 작성자 확인
        if (!(review.getUserEntity().getId()==(userId))) {
            throw new IllegalArgumentException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }
        
        Long dataId = review.getDataEntity().getId();
        
        // 리뷰 삭제 (하드 삭제)
        reviewRepository.delete(review);
        
        // 데이터의 평균 평점 업데이트
        updateDataAverageRating(dataId);
    }

    /**
     * 특정 데이터의 모든 리뷰 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<ReviewEntity> getReviewsByDataId(Long dataId, Pageable pageable) {
        return reviewRepository.findByDataEntityIdOrderByCreatedAtDesc(dataId, pageable);
    }

    /**
     * 특정 사용자의 모든 리뷰 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewEntity> getReviewsByUserId(int userId) {
        return reviewRepository.findByUserEntityIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 특정 리뷰 상세 조회
     */
    @Transactional(readOnly = true)
    public Optional<ReviewEntity> getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }

    /**
     * 사용자가 특정 데이터에 작성한 리뷰 조회
     */
    @Transactional(readOnly = true)
    public Optional<ReviewEntity> getUserReviewForData(int userId, Long dataId) {
        return reviewRepository.findByUserEntityIdAndDataEntityId(userId, dataId);
    }

    /**
     * 특정 데이터의 리뷰 통계 조회
     */
    @Transactional(readOnly = true)
    public ReviewStats getReviewStats(Long dataId) {
        List<ReviewEntity> reviews = reviewRepository.findByDataEntityId(dataId);
        
        if (reviews.isEmpty()) {
            return new ReviewStats(0, 0.0, 0, 0, 0, 0, 0);
        }
        
        int totalCount = reviews.size();
        double averageRating = reviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .average()
                .orElse(0.0);
        
        // 별점별 개수 계산
        int[] ratingCounts = new int[6]; // 인덱스 0은 사용하지 않음, 1-5만 사용
        reviews.forEach(review -> ratingCounts[review.getRating()]++);
        
        return new ReviewStats(totalCount, averageRating, 
                ratingCounts[5], ratingCounts[4], ratingCounts[3], 
                ratingCounts[2], ratingCounts[1]);
    }

    /**
     * 데이터의 평균 평점 업데이트
     */
    private void updateDataAverageRating(Long dataId) {
        List<ReviewEntity> reviews = reviewRepository.findByDataEntityId(dataId);
        
        double averageRating = reviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .average()
                .orElse(0.0);
        
        int reviewCount = reviews.size();
        
        // DataEntity의 rating과 reviewCount 업데이트
        dataRepository.findById(dataId).ifPresent(data -> {
            data.setRating(averageRating);
            data.setReviewCount(reviewCount);
            dataRepository.save(data);
        });
    }

    /**
     * 리뷰 통계 내부 클래스
     */
    public static class ReviewStats {
        private final int totalCount;
        private final double averageRating;
        private final int fiveStarCount;
        private final int fourStarCount;
        private final int threeStarCount;
        private final int twoStarCount;
        private final int oneStarCount;

        public ReviewStats(int totalCount, double averageRating, 
                          int fiveStarCount, int fourStarCount, int threeStarCount, 
                          int twoStarCount, int oneStarCount) {
            this.totalCount = totalCount;
            this.averageRating = Math.round(averageRating * 10.0) / 10.0; // 소수점 1자리
            this.fiveStarCount = fiveStarCount;
            this.fourStarCount = fourStarCount;
            this.threeStarCount = threeStarCount;
            this.twoStarCount = twoStarCount;
            this.oneStarCount = oneStarCount;
        }

        // Getters
        public int getTotalCount() { return totalCount; }
        public double getAverageRating() { return averageRating; }
        public int getFiveStarCount() { return fiveStarCount; }
        public int getFourStarCount() { return fourStarCount; }
        public int getThreeStarCount() { return threeStarCount; }
        public int getTwoStarCount() { return twoStarCount; }
        public int getOneStarCount() { return oneStarCount; }
    }

}
