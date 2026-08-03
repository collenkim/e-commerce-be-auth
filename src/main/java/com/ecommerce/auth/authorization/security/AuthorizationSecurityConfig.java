package com.ecommerce.auth.authorization.security;

import com.ecommerce.auth.sociallogin.SocialLoginProperties;
import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.security.JwtAuthenticationFilter;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 최종 {@link SecurityFilterChain}(Order 2) — {@code oauth2LoginFilterChain}(Order 1, SocialLogin
 * Unit 소유)을 제외한 모든 경로를 다룬다. Token Unit의 {@link JwtAuthenticationFilter}를 여기서
 * 최초로 배선한다(US-603, deny-by-default).
 */
@Configuration
public class AuthorizationSecurityConfig {

    /** 공개 엔드포인트 목록 — functional-design/domain-entities.md에서 확정(Q2:A). */
    private static final String[] PUBLIC_PATHS = {
        "/api/auth/signup",
        "/api/auth/login",
        "/api/auth/email/verify",
        "/api/auth/email/verify/resend",
        "/api/auth/password-reset/request",
        "/api/auth/password-reset/execute",
        "/api/auth/token/refresh",
        "/api/auth/logout",
        "/internal/tokens/validate",
        "/api/auth/social/link/confirm",
        // springdoc-openapi (Swagger UI) - API 문서 조회용, 인증 불필요
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    @Bean
    @Order(2)
    public SecurityFilterChain finalSecurityFilterChain(
            HttpSecurity http, TokenIssuanceService tokenIssuanceService, SocialLoginProperties socialLoginProperties)
            throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource(socialLoginProperties)))
                .csrf(csrf -> csrf.disable()) // Bearer 토큰 기반 stateless API - 쿠키 기반 세션이 없어 CSRF 대상 아님
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                                        .accessDeniedHandler(new CustomAccessDeniedHandler()))
                .addFilterBefore(
                        new JwtAuthenticationFilter(tokenIssuanceService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource(SocialLoginProperties socialLoginProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(originOf(socialLoginProperties.frontendRedirectUri())));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false); // Authorization 헤더 기반, 쿠키 불필요

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static String originOf(String uri) {
        URI parsed = URI.create(uri);
        int port = parsed.getPort();
        return parsed.getScheme() + "://" + parsed.getHost() + (port > 0 ? ":" + port : "");
    }
}
