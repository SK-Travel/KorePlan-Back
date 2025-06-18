package com.koreplan.user.oauth2.dto;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.koreplan.common.EncryptUtils;
import com.koreplan.common.JwtTokenProvider;
import com.koreplan.user.entity.UserEntity;
import com.koreplan.user.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor

//OAuth2 로그인 성공 후 처리 핸들러 - 로그인 성공 후 사용자 DB 등록 및 JWT 발급 후 리다이렉트 처리
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
	
	@Autowired
	private EncryptUtils encryptUtils;
	
	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		
		// OAuth2 로그인 후, 인증된 사용자 정보 가져오기
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		// 카카오/구글 공통 이메일과 이름 파싱 (attributes 구조가 다를 수 있으므로 직접 추출)
		String email = null;
		String name = null;
		String provider = (String) oAuth2User.getAttribute("provider");
		// getAttribute("email") ==  getAttributes().get("email")
		
		//카카오 인증 받은 후
		if ("kakao".equals(provider)) {
		    Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes();
		    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
		    email = (String) kakaoAccount.get("email");
		    name = (String) profile.get("nickname");

       	} else if ("naver".equals(provider)) {
            Map<String, Object> naverAttrs = oAuth2User.getAttributes();
            email = (String) naverAttrs.get("email");
            name = (String) naverAttrs.get("name");

        } else if ("google".equals(provider)) {
            email = (String) oAuth2User.getAttribute("email");
            name = (String) oAuth2User.getAttribute("name");
        }
		
	    // DB에 사용자 존재 여부 확인
		Optional<UserEntity> userOptional = userRepository.findByEmail(email);
		
		String redirectType;
		UserEntity savedUser; // ✅ 저장된 사용자 정보를 받기 위한 변수
		
		// DB에 사용자가 없으면 새로 등록
        if (userOptional.isEmpty()) {
            String generatedLoginId = generateUniqueLoginId(email, provider + "_");

            UserEntity newUser = UserEntity.builder()
                    .loginId(generatedLoginId)
                    .email(email)
                    .name(name)
                    .password(encryptUtils.hashPassword("OAuth"))
                    .phoneNumber("")
                    .build();

            savedUser = userRepository.save(newUser); // ✅ 저장된 사용자 정보 받기
            redirectType = "signup"; // 회원가입 타입
        } else {
            savedUser = userOptional.get(); // ✅ 기존 사용자 정보 받기
            redirectType = "login";   // 로그인 타입
        }

        // JWT 생성
        String token = jwtTokenProvider.generateToken(email);
        
        // ✅ 세션에 userId 포함하여 저장 (좋아요 API에서 필요)
        HttpSession session = request.getSession();
        session.setAttribute("userId", savedUser.getId()); // 👈 중요: userId 추가
        session.setAttribute("email", email);
        session.setAttribute("name", name);
        
        // 디버그 로그 추가
        System.out.println("=== OAuth2 로그인 세션 저장 ===");
        System.out.println("userId: " + savedUser.getId());
        System.out.println("email: " + email);
        System.out.println("name: " + name);
        System.out.println("sessionId: " + session.getId());
        
        // 이메일, 이름, 타입, 토큰 쿼리파라미터로 넘김
        String redirectUrl = String.format("http://localhost:5173/oauth2/redirection?email=%s&name=%s&type=%s&token=%s",
                java.net.URLEncoder.encode(email, "UTF-8"),
                java.net.URLEncoder.encode(name, "UTF-8"),
                redirectType,
                token);
        
        response.setCharacterEncoding("UTF-8");
        response.sendRedirect(redirectUrl);
	}
	
	
	// 중복되지 않는 고유 로그인 아이디 자동생성
	  private String generateUniqueLoginId(String email, String prefix) {
	        String domain = email.split("@")[1].split("\\.")[0]; // ex: gmail
	        String baseLoginId = prefix + email.split("@")[0] + "_" + domain;
	        String loginId = baseLoginId;
	        int count = 1;

	        while (userRepository.existsByLoginId(loginId)) {
	            loginId = baseLoginId + count;
	            count++;
	        }

	        return loginId;
	    }
	
}