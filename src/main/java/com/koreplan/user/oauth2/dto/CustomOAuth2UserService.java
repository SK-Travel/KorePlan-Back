package com.koreplan.user.oauth2.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.koreplan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

// OAuth2UserService 
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;

	// OAuth 로그인 시 호출되는 메서드
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		// Google 등 OAuth 제공자에서 사용자 정보 받아오기
		OAuth2User oAuth2User = super.loadUser(userRequest);
		System.out.println("✅ OAuth2User attributes: " + oAuth2User.getAttributes().containsKey("response"));
		
		// 카카오, 네이버
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		// 구글
		Map<String, Object> attributes = oAuth2User.getAttributes();

		if ("naver".equals(registrationId)) {
			Map<String, Object> responseMap = (Map<String, Object>) attributes.get("response");
			// 읽기전용 맵 복사
			Map<String, Object> modifiableResponseMap = new HashMap<>(responseMap);
			modifiableResponseMap.put("provider", "naver");

			return new DefaultOAuth2User(
				Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
				modifiableResponseMap,
				"id"
			);
		} 
		else if ("kakao".equals(registrationId)) {
			Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
			// 읽기전용 맵 복사
			Map<String, Object> modifiableKakaoAccount = new HashMap<>(kakaoAccount);
			modifiableKakaoAccount.put("provider", "kakao");

			return new DefaultOAuth2User(
				Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
				modifiableKakaoAccount,
				"email"
			);
		} 
		else {  // google
			// 읽기전용 맵 복사
			Map<String, Object> modifiableAttributes = new HashMap<>(attributes);
			modifiableAttributes.put("provider", "google");

			return new DefaultOAuth2User(
				Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
				modifiableAttributes,
				"email"
			);
		}
	}
}
