package com.koreplan.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class EncryptUtils {
	
	//BCrpyt로 하는 게 대세고, 60바이트라 안전해서, pom.xml에 삼총사 추가함
	
	// 비밀번호 정의
	private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	
	//비밀번호 해싱 60바이트짜리 생성(회원가입할 때)
	public static String hashPassword(String password) {
		return encoder.encode(password);
	}
	
	// 입력된 비밀번호, 암호화되어 저장된 비밀번호(로그인할 때)
	public static boolean checkPassword(String rawPassword, String hashedPassword) {
		return encoder.matches(rawPassword, hashedPassword);
	}
	
}
