package com.koreplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.koreplan.common.JwtAuthenticationFilter;
import com.koreplan.user.oauth2.dto.CustomOAuth2UserService;
import com.koreplan.user.oauth2.dto.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
   
   private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
   private final JwtAuthenticationFilter jwtAuthenticationFilter;
   private final CustomOAuth2UserService customOAuth2UserDTO;
   private final ClientRegistrationRepository clientRegistrationRepository;

   @Bean
   public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
       return config.getAuthenticationManager();
   }
   
   @Bean
   public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       http
           .csrf(csrf -> csrf.disable())
           .sessionManagement(session ->
               session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
           .authorizeHttpRequests(authz -> authz
               .requestMatchers(
                   "/signIn",
                   "/signup", 
                   "/oauth2/**",
                   "/login/oauth2/code/**",
                   "/public/**",
                   "/api/**",
                   "/swagger-ui/**",
                   "/swagger-ui.html",
                   "/v3/api-docs/**",
                   "/swagger-resources/**",
                   "/webjars/**"
               )
               .permitAll()
               .anyRequest().authenticated()
           )
           .exceptionHandling(exception -> 
               exception.authenticationEntryPoint((request, response, authException) -> {
                   response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                   response.setContentType("application/json");
                   response.getWriter().write("{\"error\": \"Unauthorized\"}");
               })
           )
           .oauth2Login(oauth2 -> oauth2
               .userInfoEndpoint(userInfo -> 
                   userInfo.userService(customOAuth2UserDTO))
               .successHandler(oAuth2LoginSuccessHandler)
           )
           .formLogin(form -> form.disable());

       http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

       return http.build();
   }
}