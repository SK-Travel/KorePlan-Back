package com.koreplan.controller.like;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
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
	
	// 좋아요 한 거
	@RequestMapping("/{dataId}")
	public Map<String, Object> likeToggle(
			@PathVariable(name = "dataId") Long dataId,
			HttpSession session,
			@AuthenticationPrincipal OAuth2User oAuthUser) {
		
		// 1. 로컬 로그인 시 저장된 이름 인증(없으면 좋아요 X)
		Integer userId = (Integer) session.getAttribute("userId");
		
		// 2. 소셜로그인 시
		if (userId == null && oAuthUser != null) {
			String email = oAuthUser.getAttribute("email");
			userId = userService.getUserIdByEmail(email);
		}
		
		Map<String, Object> result = new HashMap<>();
		
		// 3. 인증 X
		if (userId == null) {
			result.put("code", 401);
			result.put("error_message", "로그인되지 않았습니다. 로그인 해주세요");
		} else {
			int likeStatus = likeService.likeToggle(dataId, userId);
			result.put("code", 200);
			result.put("likeStatus", likeStatus); // 1이면 좋아요 됨, 0이면 해제됨
			result.put("message", likeStatus == 1 ? "찜 추가됨" : "찜 취소됨");
		}
		return result;
	}
	
}
