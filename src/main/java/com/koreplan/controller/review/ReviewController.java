package com.koreplan.controller.review;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.review.ReviewDto;
import com.koreplan.dto.review.ReviewReadDto;
import com.koreplan.entity.review.ReviewEntity;
import com.koreplan.service.review.ReviewService;
import com.koreplan.service.review.ReviewService.ReviewStats;
import com.koreplan.user.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @Autowired
    private UserService userService;

    /**
     * 리뷰 작성
     */
    @PostMapping
    public Map<String, Object> createReview(
            @RequestBody ReviewCreateRequest request,
            HttpSession session,
            @AuthenticationPrincipal OAuth2User oAuthUser) {
        
        System.out.println("=== 리뷰 작성 ===");
        System.out.println("요청 데이터: " + request);
        
        // 사용자 인증
        Integer userId = getUserId(session, oAuthUser);
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            System.out.println("❌ 인증 실패 - userId가 null");
            result.put("code", 401);
            result.put("error_message", "로그인되지 않았습니다. 로그인 해주세요");
            return result;
        }
        
        try {
            System.out.println("✅ 인증 성공 - userId: " + userId);
            
            // 이미 리뷰를 작성했는지 확인
            Optional<ReviewEntity> existingReview = reviewService.getUserReviewForData(userId, request.getDataId());
            if (existingReview.isPresent()) {
                result.put("code", 409);
                result.put("error_message", "이미 해당 데이터에 리뷰를 작성하셨습니다.");
                return result;
            }
            
            ReviewEntity review = reviewService.createReview(
                request.getDataId(), 
                userId, 
                request.getRating(), 
                request.getContent()
            );
            
            result.put("code", 200);
            result.put("message", "리뷰가 성공적으로 작성되었습니다.");
            result.put("reviewId", review.getId());
            
            System.out.println("리뷰 작성 완료 - reviewId: " + review.getId());
            
        } catch (IllegalArgumentException e) {
            System.err.println("리뷰 작성 오류: " + e.getMessage());
            result.put("code", 400);
            result.put("error_message", e.getMessage());
        } catch (Exception e) {
            System.err.println("리뷰 작성 시스템 오류: " + e.getMessage());
            result.put("code", 500);
            result.put("error_message", "리뷰 작성 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 리뷰 수정 - TODO: 구현 예정
     */
    @PutMapping("/{reviewId}")
    public Map<String, Object> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequest request,
            HttpSession session,
            @AuthenticationPrincipal OAuth2User oAuthUser) {
        
        System.out.println("=== 리뷰 수정 ===");
        System.out.println("reviewId: " + reviewId);
        System.out.println("요청 데이터: " + request);
        
        // 사용자 인증
        Integer userId = getUserId(session, oAuthUser);
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            System.out.println("❌ 인증 실패 - userId가 null");
            result.put("code", 401);
            result.put("error_message", "로그인되지 않았습니다. 로그인 해주세요");
            return result;
        }
        
        try {
            System.out.println("✅ 인증 성공 - userId: " + userId);
            
            ReviewEntity updatedReview = reviewService.updateReview(
                reviewId, 
                userId, 
                request.getRating(), 
                request.getContent()
            );
            
            result.put("code", 200);
            result.put("message", "리뷰가 성공적으로 수정되었습니다.");
            result.put("reviewId", updatedReview.getId());
            
            System.out.println("리뷰 수정 완료 - reviewId: " + updatedReview.getId());
            
        } catch (IllegalArgumentException e) {
            System.err.println("리뷰 수정 오류: " + e.getMessage());
            result.put("code", 400);
            result.put("error_message", e.getMessage());
        } catch (Exception e) {
            System.err.println("리뷰 수정 시스템 오류: " + e.getMessage());
            result.put("code", 500);
            result.put("error_message", "리뷰 수정 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 리뷰 삭제 - TODO: 구현 예정
     */
    @DeleteMapping("/{reviewId}")
    public Map<String, Object> deleteReview(
            @PathVariable Long reviewId,
            HttpSession session,
            @AuthenticationPrincipal OAuth2User oAuthUser) {
        
        System.out.println("=== 리뷰 삭제 ===");
        System.out.println("reviewId: " + reviewId);
        
        // 사용자 인증
        Integer userId = getUserId(session, oAuthUser);
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            System.out.println("❌ 인증 실패 - userId가 null");
            result.put("code", 401);
            result.put("error_message", "로그인되지 않았습니다. 로그인 해주세요");
            return result;
        }
        
        try {
            System.out.println("✅ 인증 성공 - userId: " + userId);
            
            reviewService.deleteReview(reviewId, userId);
            
            result.put("code", 200);
            result.put("message", "리뷰가 성공적으로 삭제되었습니다.");
            
            System.out.println("리뷰 삭제 완료 - reviewId: " + reviewId);
            
        } catch (IllegalArgumentException e) {
            System.err.println("리뷰 삭제 오류: " + e.getMessage());
            result.put("code", 400);
            result.put("error_message", e.getMessage());
        } catch (Exception e) {
            System.err.println("리뷰 삭제 시스템 오류: " + e.getMessage());
            result.put("code", 500);
            result.put("error_message", "리뷰 삭제 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 특정 데이터의 리뷰 조회 (페이징) - 인증 불필요
     */
    @GetMapping("/data/{dataId}")
    public ResponseEntity<Page<ReviewReadDto>> getReviewsByDataId(
            @PathVariable Long dataId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        System.out.println("=== 데이터별 리뷰 조회 ===");
        System.out.println("dataId: " + dataId);
        
        Page<ReviewReadDto> reviewDtos = reviewService.getReviewsDtoByDataId(dataId, pageable);
        
        return ResponseEntity.ok(reviewDtos);
    }

    /**
     * 특정 데이터의 리뷰 통계 조회 - 인증 불필요
     */
    @GetMapping("/stats/{dataId}")
    public ResponseEntity<ReviewStats> getReviewStats(@PathVariable Long dataId) {
        
        System.out.println("=== 리뷰 통계 조회 ===");
        System.out.println("dataId: " + dataId);
        
        ReviewStats stats = reviewService.getReviewStats(dataId);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 현재 로그인한 사용자의 모든 리뷰 조회
     */
    @PostMapping("/my-reviews")
    public Map<String, Object> getMyReviews(
        HttpSession session,
        @AuthenticationPrincipal OAuth2User oAuthUser) {
        
        System.out.println("=== 내 리뷰 목록 조회 ===");
        
        // 사용자 인증
        Integer userId = getUserId(session, oAuthUser);
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            System.out.println("❌ 인증 실패 - userId가 null");
            result.put("code", 401);
            result.put("error_message", "로그인되지 않았습니다. 로그인 해주세요");
        } else {
            try {
                System.out.println("✅ 인증 성공 - userId: " + userId);
                List<ReviewReadDto> reviewDtos = reviewService.getReviewsDtoByUserId(userId);
                
                result.put("code", 200);
                result.put("reviews", reviewDtos);
                result.put("message", "리뷰 목록 조회 성공");
                result.put("count", reviewDtos.size());
                System.out.println("사용자 " + userId + "의 리뷰 개수: " + reviewDtos.size());
            } catch (Exception e) {
                System.err.println("리뷰 목록 조회 오류: " + e.getMessage());
                result.put("code", 500);
                result.put("error_message", "리뷰 목록 조회 중 오류 발생");
            }
        }
        
        return result;
    }

    /**
     * 사용자 ID 추출 헬퍼 메서드
     */
    private Integer getUserId(HttpSession session, OAuth2User oAuthUser) {
        Integer userId = (Integer) session.getAttribute("userId");
        System.out.println("세션에서 가져온 userId: " + userId);
        
        // 소셜로그인 시
        if (userId == null && oAuthUser != null) {
            String email = oAuthUser.getAttribute("email");
            System.out.println("OAuth2 이메일로 사용자 조회: " + email);
            userId = userService.getUserIdByEmail(email);
            System.out.println("조회된 userId: " + userId);
        }
        
        return userId;
    }

    /**
     * 리뷰 작성 요청 DTO
     */
    public static class ReviewCreateRequest {
        private Long dataId;
        private int rating;
        private String content;
        
        // Getters and Setters
        public Long getDataId() { return dataId; }
        public void setDataId(Long dataId) { this.dataId = dataId; }
        
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        @Override
        public String toString() {
            return "ReviewCreateRequest{" +
                    "dataId=" + dataId +
                    ", rating=" + rating +
                    ", content='" + content + '\'' +
                    '}';
        }
    }

    /**
     * 리뷰 수정 요청 DTO
     */
    public static class ReviewUpdateRequest {
        private int rating;
        private String content;
        
        // Getters and Setters
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        @Override
        public String toString() {
            return "ReviewUpdateRequest{" +
                    "rating=" + rating +
                    ", content='" + content + '\'' +
                    '}';
        }
    }
}