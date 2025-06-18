package com.koreplan.controller.like;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.service.like.LikeService;
import com.koreplan.user.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/like")
public class LikeRestController {

	@Autowired
	private UserService userService;

	@Autowired
	private LikeService likeService;

	// 좋아요 토글
	@PostMapping("/{dataId}")
	public Map<String, Object> likeToggle(
			@PathVariable(name = "dataId") Long dataId,
			HttpSession session,
			@AuthenticationPrincipal OAuth2User oAuthUser) {

		// 디버그 로그 추가
		System.out.println("=== 좋아요 API 디버깅 ===");
		System.out.println("Session ID: " + session.getId());
		System.out.println("Session userId: " + session.getAttribute("userId"));
		System.out.println("OAuth User: " + (oAuthUser != null ? oAuthUser.getAttribute("email") : "null"));

		// 1. 로컬 로그인 시 저장된 이름 인증(없으면 좋아요 X)
		Integer userId = (Integer) session.getAttribute("userId");
		System.out.println("세션에서 가져온 userId: " + userId);

		// 2. 소셜로그인 시 (OAuth2User가 있는 경우)
		if (userId == null && oAuthUser != null) {
			String email = oAuthUser.getAttribute("email");
			System.out.println("OAuth2 이메일로 사용자 조회: " + email);
			userId = userService.getUserIdByEmail(email);
			System.out.println("조회된 userId: " + userId);
		}

		Map<String, Object> result = new HashMap<>();

		// 3. 인증 확인
		if (userId == null) {
			System.out.println("❌ 인증 실패 - userId가 null");
			result.put("code", 401);
			result.put("error_message", "로그인되지 않았습니다. 로그인 해주세요");
		} else {
			System.out.println("✅ 인증 성공 - userId: " + userId + ", dataId: " + dataId);
			boolean likeStatus = likeService.likeToggle(dataId, userId);
			result.put("code", 200);
			result.put("likeStatus", likeStatus);
			result.put("message", likeStatus  ? "찜 추가됨" : "찜 취소됨");
			System.out.println("좋아요 처리 결과: " + likeStatus);
		}
		return result;
	}

	// 현재 사용자의 모든 좋아요 목록 조회 
	@GetMapping("/my-likes")
	public Map<String, Object> getMyLikes(
			HttpSession session,
			@AuthenticationPrincipal OAuth2User oAuthUser) {

		System.out.println("=== 내 좋아요 목록 조회 ===");

		// 사용자 인증
		Integer userId = (Integer) session.getAttribute("userId");
		if (userId == null && oAuthUser != null) {
			String email = oAuthUser.getAttribute("email");
			userId = userService.getUserIdByEmail(email);
		}

		Map<String, Object> result = new HashMap<>();

		if (userId == null) {
			result.put("code", 401);
			result.put("error_message", "로그인되지 않았습니다");
		} else {
			try {
				Set<Long> likedDataIds = likeService.getUserLikedDataIds(userId);
				result.put("code", 200);
				result.put("likedDataIds", likedDataIds);
				result.put("message", "좋아요 목록 조회 성공");
				System.out.println("사용자 " + userId + "의 좋아요 목록: " + likedDataIds);
			} catch (Exception e) {
				System.err.println("좋아요 목록 조회 오류: " + e.getMessage());
				result.put("code", 500);
				result.put("error_message", "좋아요 목록 조회 중 오류 발생");
			}
		}

		return result;
	}

	
	@PostMapping("/test/{dataId}/{userId}")
	public Map<String, Object> likeToggleTest(
	        @PathVariable(name = "dataId") Long dataId,
	        @PathVariable(name = "userId") Integer userId) {
	    
	    Map<String, Object> result = new HashMap<>();
	    
	    try {
	        boolean likeStatus = likeService.likeToggle(dataId, userId);
	        result.put("code", 200);
	        result.put("likeStatus", likeStatus);
	        result.put("message", likeStatus == true ? "찜 추가됨" : "찜 취소됨");
	    } catch (Exception e) {
	        result.put("code", 500);
	        result.put("error_message", "서버 오류: " + e.getMessage());
	    }
	    
	    return result;
	}
	



	//여러 데이터의 좋아요 상태 확인
	@PostMapping("/check-status")
	public Map<String, Object> checkLikeStatus(
			@RequestBody Map<String, List<Long>> request,
			HttpSession session,
			@AuthenticationPrincipal OAuth2User oAuthUser) {

		System.out.println("=== 좋아요 상태 확인 ===");

		List<Long> dataIds = request.get("dataIds");
		System.out.println("확인할 dataIds: " + dataIds);

		// 사용자 인증
		Integer userId = (Integer) session.getAttribute("userId");
		if (userId == null && oAuthUser != null) {
			String email = oAuthUser.getAttribute("email");
			userId = userService.getUserIdByEmail(email);
		}

		Map<String, Object> result = new HashMap<>();

		if (userId == null) {
			result.put("code", 401);
			result.put("error_message", "로그인되지 않았습니다");
		} else if (dataIds == null || dataIds.isEmpty()) {
			result.put("code", 400);
			result.put("error_message", "dataIds가 필요합니다");
		} else {
			try {
				Map<Long, Boolean> likeStatusMap = likeService.checkLikeStatus(userId, dataIds);
				result.put("code", 200);
				result.put("likeStatusMap", likeStatusMap);
				result.put("message", "좋아요 상태 확인 성공");
				System.out.println("좋아요 상태 결과: " + likeStatusMap);
			} catch (Exception e) {
				System.err.println("좋아요 상태 확인 오류: " + e.getMessage());
				result.put("code", 500);
				result.put("error_message", "좋아요 상태 확인 중 오류 발생");
			}
		}

		return result;
	}
}

