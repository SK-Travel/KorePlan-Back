package com.koreplan.user.google.dto;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.koreplan.common.JwtTokenProvider;
import com.koreplan.user.entity.UserEntity;
import com.koreplan.user.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
	
	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;
	
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		
		// OAuth2 로그인 후, 인증된 사용자 정보 가져오기
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		String email = oAuth2User.getAttribute("email");
		String name = oAuth2User.getAttribute("name");
		
		Optional<UserEntity> userOptional = userRepository.findByEmail(email);
		
		// DB에 사용자가 없으면 새로 등록
        if (userOptional.isEmpty()) {
            String generatedLoginId = "google_" + email.split("@")[0];

            UserEntity newUser = UserEntity.builder()
                    .loginId(generatedLoginId)
                    .email(email)
                    .name(name)
                    .password("") // 비워둠
                    .phoneNumber("") // 기본값
                    .build();

            userRepository.save(newUser);
        }

        // JWT 생성 및 리다이렉트
        // JWT 생성
        String token = jwtTokenProvider.generateToken(email);
        // 리다이렉트 URL
        String redirectUrl = "http://localhost:5173/oauth2/redirection?token=" + token;
        response.setCharacterEncoding("UTF-8");
        response.sendRedirect(redirectUrl);
	}
	

    // 로그인 ID 자동 생성
    private String generateLoginId(String email) {
        String prefix = "google_";
        return prefix + email.split("@")[0];
    }
	
}
