package com.koreplan.user.google;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreplan.common.JwtTokenProvider;
import com.koreplan.user.google.entity.OAuth2UserInfoEntity;
import com.koreplan.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2RestController {
	
	  	private final UserRepository userRepository;
	    private final JwtTokenProvider jwtTokenProvider;

	    @GetMapping("/redirect")
	    public ResponseEntity<?> handleRedirect(HttpServletRequest request) {
	        // Spring Security가 세션에 저장한 사용자 정보 추출
	        OAuth2UserInfoEntity oAuth2UserInfoEntity = (OAuth2UserInfoEntity) request.getSession().getAttribute("oauth2UserInfo");

	        if (oAuth2UserInfoEntity == null) {
	            return ResponseEntity.badRequest().body("OAuth2 사용자 정보가 없습니다.");
	        }

	        Optional<String> existingUserId = userRepository.findUserIdByEmail(oAuth2UserInfoEntity.getEmail());

	        if (existingUserId.isPresent()) {
	            // 기존 사용자: 토큰 생성 후 응답
	            String token = jwtTokenProvider.generateToken(existingUserId.get());
	            return ResponseEntity.ok().body(
	                    new OAuth2LoginResponse(token, null)
	            );
	        } else {
	            // 신규 사용자: 프론트에 가입 유도
	            return ResponseEntity.ok().body(
	                    new OAuth2LoginResponse(null, "/signUp?email=" + oAuth2UserInfoEntity.getEmail() + "&from=google")
	            );
	        }
	    }

	    // 응답 DTO
	    public record OAuth2LoginResponse(String token, String redirectUrl) {}
}

