package com.koreplan.user.google.dto;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.koreplan.common.EncryptUtils;
import com.koreplan.user.entity.UserEntity;
import com.koreplan.user.google.entity.OAuth2UserInfoEntity;
import com.koreplan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserDTO extends DefaultOAuth2UserService {
	
	private final UserRepository userRepository;
	
	// OAuth 로그인 시 호출되는 메서드
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
		// Google 등 OAuth 제공자에서 사용자 정보 받아오기
		OAuth2User oAuth2User = super.loadUser(userRequest);
		 // 받아온 사용자 정보에서 email, name 추출
		OAuth2UserInfoEntity userInfo = new OAuth2UserInfoEntity(oAuth2User.getAttributes());
		
		// DB에 이미 가입된 사용자인지 확인
		Optional<UserEntity> optionalUser = userRepository.findByEmail(userInfo.getEmail());
		
		if(optionalUser.isPresent()) {
	        // 이미 가입된 사용자 → 로그인 처리
	        return new DefaultOAuth2User(
	            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
	            oAuth2User.getAttributes(),
	            "email");
		} else {
			 // 신규 사용자 → 프론트로 리다이렉트해서 회원가입 유도
	        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/sign-up")
	            .queryParam("email", userInfo.getEmail())
	            .queryParam("name", userInfo.getName())
	            .build()
	            .toUriString();

            // 예외를 던지지 않고 리다이렉트 URL을 반환
            return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(),
                "email");
//            // 예외를 던지지 않고 리다이렉트 URL 반환
//            throw new OAuth2AuthenticationException(
//                new OAuth2Error("redirect", "New user found, redirecting to sign-up page.", redirectUrl)
//            );
		}
	}
	
	// 새로운 OAuth 사용자를 DB에 등록
	private UserEntity registerNewUser(OAuth2UserInfoEntity userInfo) {
		String randomLoginId = "goolge_" + UUID.randomUUID().toString().substring(0, 8); // 유니크한 로그인 ID
		String phonePlaceHolder = "000-0000-0000"; // 전화번호는 Google OAuth에서 제공되지 않음
		
		UserEntity newUser = UserEntity.builder()
	                .loginId(randomLoginId)
	                .password(EncryptUtils.hashPassword("oauth")) // 기존에 만든 해시 유틸리티로 비밀번호 해싱
	                .name(userInfo.getName())
	                .email(userInfo.getEmail())
	                .phoneNumber(phonePlaceHolder)
	                .build();
		
		return userRepository.save(newUser); // DB저장
	}
}
