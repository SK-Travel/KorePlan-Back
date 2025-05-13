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
         .csrf().disable() // CSRF 비활성화 (운영 시 주의)
         .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 대신 JWT 사용
         .and()
         .authorizeHttpRequests()
             .requestMatchers("/signIn", "/signup", "/oauth2/**").permitAll() // 허용 경로
             .anyRequest().authenticated()
         .and()
         .oauth2Login()
             .loginPage("/signIn") // 커스텀 로그인 페이지 URL
             .userInfoEndpoint()
                 .userService(customOAuth2UserDTO) // OAuth 사용자 정보 처리
             .and()
             .successHandler(oAuth2LoginSuccessHandler) // ✅ 커스텀 성공 핸들러 등록
         .and()
         .formLogin().disable(); // 기본 폼 로그인 비활성화

     // JWT 필터 등록
     http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

     return http.build();
	}
}
