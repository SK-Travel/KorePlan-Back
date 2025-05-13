package com.koreplan.common;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {
        // 1. 요청 헤더에서 Authorization 값 추출
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // "Bearer " 다음부터 자르기

            try {
                // 2. 토큰 파싱 및 검증
                Claims claims = Jwts.parser()
                        .setSigningKey("your-secret-key".getBytes()) // JwtTokenProvider와 동일한 키
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.getSubject();

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 3. 인증 객체 생성 후 컨텍스트에 저장
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    new User(email, "", Collections.emptyList()), // UserDetails 대체
                                    null,
                                    Collections.emptyList());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (SignatureException e) {
                // 유효하지 않은 서명일 경우
                System.out.println("Invalid JWT signature: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("JWT parsing error: " + e.getMessage());
            }
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
