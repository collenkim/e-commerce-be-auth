package com.ecommerce.auth.sociallogin.security;

import com.ecommerce.auth.sociallogin.NormalizingOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@code /oauth2/**}, {@code /login/**}(Spring Security가 자동 제공하는 OAuth2 인가/콜백 경로) 전용
 * {@link SecurityFilterChain}(Order 1)을 등록한다. 나머지 모든 경로를 다루는 필터체인(Order 2)은
 * {@code com.ecommerce.auth.authorization.AuthorizationSecurityConfig}가 소유한다(원래 이 클래스에
 * 있던 임시 스캐폴딩 {@code temporaryOpenFilterChain}을 Authorization Unit이 완전히 대체함).
 */
@Configuration
public class SocialLoginSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2LoginFilterChain(
            HttpSecurity http,
            NormalizingOAuth2UserService normalizingOAuth2UserService,
            SocialLoginSuccessHandler successHandler,
            SocialLoginFailureHandler failureHandler)
            throws Exception {
        http.securityMatcher("/oauth2/**", "/login/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(
                        oauth2 ->
                                oauth2
                                        .userInfoEndpoint(userInfo -> userInfo.userService(normalizingOAuth2UserService))
                                        .successHandler(successHandler)
                                        .failureHandler(failureHandler));
        return http.build();
    }
}
