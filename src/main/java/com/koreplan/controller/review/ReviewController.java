package com.koreplan.controller.review;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.dto.review.ReviewDto;
import com.koreplan.service.review.ReviewService;
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
     * 특정 데이터의 리뷰 조회 (페이징) - 인증 불필요
     */
    @GetMapping("/data/{dataId}")
    public ResponseEntity<Page<ReviewDto>> getReviewsByDataId(
            @PathVariable Long dataId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        System.out.println("=== 데이터별 리뷰 조회 ===");
        System.out.println("dataId: " + dataId);
        
        Page<ReviewDto> reviewDtos = reviewService.getReviewsDtoByDataId(dataId, pageable);
        
        return ResponseEntity.ok(reviewDtos);
    }

    /**
     * 현재 로그인한 사용자의 모든 리뷰 조회
     */
    @GetMapping("/my-reviews")
    public Map<String, Object> getMyReviews(
            HttpSession session,
            @AuthenticationPrincipal OAuth2User oAuthUser) {
        
        System.out.println("=== 내 리뷰 목록 조회 ===");
        
        // 사용자 인증 (좋아요 컨트롤러와 동일한 방식)
        Integer userId = (Integer) session.getAttribute("userId");
        System.out.println("세션에서 가져온 userId: " + userId);
        
        // 소셜로그인 시
        if (userId == null && oAuthUser != null) {
            String email = oAuthUser.getAttribute("email");
            System.out.println("OAuth2 이메일로 사용자 조회: " + email);
            userId = userService.getUserIdByEmail(email);
            System.out.println("조회된 userId: " + userId);
        }
        
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            System.out.println("❌ 인증 실패 - userId가 null");
            result.put("code", 401);
            result.put("error_message", "로그인되지 않았습니다. 로그인 해주세요");
        } else {
            try {
                System.out.println("✅ 인증 성공 - userId: " + userId);
                List<ReviewDto> reviewDtos = reviewService.getReviewsDtoByUserId(userId);
                
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
}