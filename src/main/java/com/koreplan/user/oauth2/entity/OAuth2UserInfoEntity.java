package com.koreplan.user.oauth2.entity;

import java.util.Map;


// OAuth2 사용자 정보를 파싱하는 클래스 - Google OAuth2 와 Kakao OAuth2 응답 구조가 다르므로 분기 처리함
public class OAuth2UserInfoEntity {
	// OAuth 사용자 정보 파싱용
	private final String email;
	private final String name;
	
    // OAuth2User의 attributes 맵을 받아서 이메일과 이름을 추출 카카오 로그인 시는 kakao_account와 profile 정보를 사용함 구글 로그인 시는 email, name 키를 바로 사용함

	// Google OAuth 응답으로부터 이메일과 이름만 추출
	public OAuth2UserInfoEntity(Map<String, Object> attributes) {
		if (attributes.containsKey("kakao_account")) {
			//카카오 로그인 사용자 정보 파싱
			Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
			Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
			
			this.email = (String) kakaoAccount.get("email");
			this.name = (String) profile.get("nickname");
			
		} else {
			// 구글 사용자 파싱
			this.email = (String) attributes.get("email");
			this.name = (String) attributes.get("name");
		}
	}
	
	public String getEmail() {return email;}
	public String getName() {return name;}
	
}