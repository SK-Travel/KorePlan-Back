package com.koreplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	// Spring Sercurity 예외 처리
	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // 개발 중 CSRF 비활성화 (주의 필요)
            .authorizeHttpRequests()
                .requestMatchers("/api/**").permitAll() // React에서 호출하는 API는 모두 허용
                .anyRequest().authenticated()
            .and()
            .formLogin().disable(); // formLogin도 꺼서 리디렉션 막기

        return http.build();
    }
}
