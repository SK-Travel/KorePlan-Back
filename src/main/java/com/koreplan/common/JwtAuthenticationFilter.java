//package com.koreplan.common;
//
//import java.io.IOException;
//import java.util.Collections;
//
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureException;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//@Component
//@RequiredArgsConstructor
//
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final JwtTokenProvider jwtTokenProvider;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//                                    throws ServletException, IOException {
//        // 1. 요청 헤더에서 Authorization 값 추출 ("Bearer 토큰")
//        String authHeader = request.getHeader("Authorization");
//        
//        // 2. Bearer 토큰 형식인지 확인
//        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
//        	// 토큰 부분만 잘라내기
//            String token = authHeader.substring(7); // "Bearer " 다음부터 자르기
//
//            try {
//            	// 3. 토큰 파싱 및 서명 검증
//            	Claims claims = Jwts.parserBuilder()
//                        .setSigningKey(jwtTokenProvider.getSecretKey()) // 프로바이더에서 키 가져옴
//                        .build()
//                        .parseClaimsJws(token) // 검증 및 파싱
//                        .getBody();
//
//            	// 토큰 내에 저장된 이메일(주체) 가져오기
//                String email = claims.getSubject();
//
//             // 4. 인증 정보가 없으면 인증 객체 생성 및 SecurityContext에 저장
//                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                    // 인증 객체 생성 후 컨텍스트에 저장
//                    UsernamePasswordAuthenticationToken authentication =
//                            new UsernamePasswordAuthenticationToken(
//                                    new User(email, "", Collections.emptyList()), // UserDetails 대체
//                                    null,
//                                    Collections.emptyList());
//                    // 추가 정보 설정
//                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                }
//
//            } catch (SecurityException e) {
//                // 유효하지 않은 서명일 경우
//                System.out.println("Invalid JWT signature: " + e.getMessage());
//            } catch (Exception e) {
//                System.out.println("JWT parsing error: " + e.getMessage());
//            }
//        }
//
//        // 다음 필터로 진행
//        filterChain.doFilter(request, response);
//    }
//}

package com.koreplan.common;

import java.io.IOException;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

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
    
    // JWT 검증을 건너뛸 경로들
    private final List<String> excludeUrlPatterns = Arrays.asList(
        "/api/",               // API 경로 전체
        "/api/region-data/",   // 명시적으로 region-data API
        "/oauth2/",            // OAuth2 경로  
        "/login",              // 로그인 페이지
        "/signIn",             // 사인인 페이지
        "/signup",             // 회원가입 페이지
        "/public/",            // 공개 경로
        "/css/",               // 정적 리소스
        "/js/",
        "/images/",
        "/favicon.ico"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestURI = request.getRequestURI();
        
        // 제외할 경로들과 매치되는지 확인
        boolean shouldSkip = excludeUrlPatterns.stream()
                .anyMatch(pattern -> requestURI.startsWith(pattern));
        
        if (shouldSkip) {
            System.out.println("JWT 필터 건너뜀: " + requestURI);
        }
        
        return shouldSkip;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {
        
        System.out.println("JWT 필터 실행: " + request.getRequestURI());
        
        // 1. 요청 헤더에서 Authorization 값 추출 ("Bearer 토큰")
        String authHeader = request.getHeader("Authorization");
        
        // 2. Bearer 토큰 형식인지 확인
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            // 토큰 부분만 잘라내기
            String token = authHeader.substring(7); // "Bearer " 다음부터 자르기
            try {
                // 3. 토큰 파싱 및 서명 검증
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(jwtTokenProvider.getSecretKey()) // 프로바이더에서 키 가져옴
                        .build()
                        .parseClaimsJws(token) // 검증 및 파싱
                        .getBody();
                        
                // 토큰 내에 저장된 이메일(주체) 가져오기
                String email = claims.getSubject();
                
                // 4. 인증 정보가 없으면 인증 객체 생성 및 SecurityContext에 저장
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 인증 객체 생성 후 컨텍스트에 저장
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    new User(email, "", Collections.emptyList()), // UserDetails 대체
                                    null,
                                    Collections.emptyList());
                    // 추가 정보 설정
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    System.out.println("JWT 인증 성공: " + email);
                }
            } catch (SecurityException e) {
                // 유효하지 않은 서명일 경우
                System.out.println("Invalid JWT signature: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("JWT parsing error: " + e.getMessage());
            }
        } else {
            System.out.println("Authorization 헤더 없음 또는 Bearer 토큰 아님");
        }
        
        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
