package com.koreplan.user.google.entity;

import java.util.Map;

public class OAuth2UserInfoEntity {
	// OAuth 사용자 정보 파싱용
	private final String email;
	private final String name;
	
	// Google OAuth 응답으로부터 이메일과 이름만 추출
	public OAuth2UserInfoEntity(Map<String, Object> attributes) { 
		this.email = (String) attributes.get("email");
		this.name = (String) attributes.get("name");
	}
	
	public String getEmail() {return email;}
	public String getName() {return name;}
	
}