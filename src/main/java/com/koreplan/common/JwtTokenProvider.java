package com.koreplan.common;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;


@Component
public class JwtTokenProvider {
	 // application.properties 에서 주입받은 Base64 인코딩된 시크릿 키
    @Value("${jwt.secret}")
    private String secretKeyBase64;
    // 실제 JWT 서명에 사용하는 Key 객체
	private Key secretKey;
	// 토큰 만료 시간 (1일, 밀리초 단위)
  
	private final long EXPIRATION_TIME = 1000 * 60 * 60 * 2; //2시간
	
	// 빈 초기화 후 실행, Base64 키를 디코딩하여 Key 객체 생성
    @PostConstruct
    protected void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 이메일을 기반으로 JWT 토큰 생성
     * @param email 사용자 이메일
     * @return JWT 토큰 문자열
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * JWT 검증 시 사용할 시크릿 키 반환
     * @return Key 객체
     */
    public Key getSecretKey() {
        return secretKey;
    }
}
