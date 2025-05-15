package com.koreplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.koreplan.common.JwtAuthenticationFilter;
import com.koreplan.user.google.dto.CustomOAuth2UserDTO;
import com.koreplan.user.google.dto.OAuth2LoginSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomOAuth2UserDTO customOAuth2UserDTO;
	
	// 인증 매니저 빈 등록
	@Bean
	public AuthenticationManager authenticationManager (AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
	
	
	// Spring Security 설정
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	  http
	    .csrf().disable()
	    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	    .and()
	    .authorizeHttpRequests()
	        .anyRequest().permitAll() // 모든 요청 허용
	    .and()
	    .oauth2Login().disable()     // 필요 없으면 비활성화
	    .formLogin().disable();      // 기본 로그인 비활성화
	
    // 필요 시 JWT 필터도 비활성화하거나 남겨둬도 됨
    // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
     return http.build();
	}
}
