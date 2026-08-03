package com.ecommerce.auth.token.security;

import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.dto.TokenValidationResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 매 요청마다 Authorization 헤더의 Access Token을 검증하고 SecurityContext를 채운다 (nfr-design-patterns.md).
 *
 * <p><b>의도적으로 Spring Bean이 아니다</b> — {@code @Component}를 붙이지 않는다. 이 필터를
 * {@code SecurityFilterChain}에 등록하고 공개 엔드포인트를 제외하는 것은 Authorization Unit(Unit 5)의
 * 책임이다 (unit-of-work.md 경계, token-code-generation-plan.md 범위 참고). Authorization Unit이
 * 이 클래스를 생성자로 주입받아 명시적으로 등록한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenIssuanceService tokenIssuanceService;

    public JwtAuthenticationFilter(TokenIssuanceService tokenIssuanceService) {
        this.tokenIssuanceService = tokenIssuanceService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String accessToken = header.substring(BEARER_PREFIX.length());
            TokenValidationResult result = tokenIssuanceService.validate(accessToken);
            if (result.valid()) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + result.role().name()));
                var authentication =
                        new UsernamePasswordAuthenticationToken(result.accountId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
