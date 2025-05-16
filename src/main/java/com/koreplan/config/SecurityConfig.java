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

        // OAuth2 요청 리졸버 커스터마이징 (구글 계정선택 항상 띄우기 위해 prompt=select_account 추가)
        OAuth2AuthorizationRequestResolver customResolver = new OAuth2AuthorizationRequestResolver() {

            private final DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                    new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request);
                return customizeAuthorizationRequest(authRequest);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request, clientRegistrationId);
                return customizeAuthorizationRequest(authRequest);
            }

            private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest request) {
                if (request == null) {
                    return null;
                }
                
                String registrationId = (String) request.getAttributes().get("registration_id");

                OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(request);

                if ("google".equals(registrationId)) {
                    builder.additionalParameters(params -> params.put("prompt", "select_account"));
                }
                else if ("naver".equals(registrationId)) {
                    builder.additionalParameters(params -> {
                        params.put("auth_type", "reauthenticate");
                        params.put("prompt", "consent");
                    });
                }

                return builder.build();
            }
        };

        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .requestMatchers(
                    "/signIn",
                    "/signup",
                    "/oauth2/**",
                    "/public/**",
                    "/api/**"
                ).permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2Login()
                .authorizationEndpoint()
                    .authorizationRequestResolver(customResolver)
                .and()
                .userInfoEndpoint()
                    .userService(customOAuth2UserDTO)
                .and()
                .successHandler(oAuth2LoginSuccessHandler)
            .and()
            .formLogin().disable();

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
