package com.ecommerce.auth.ratelimit.filter;

import com.ecommerce.auth.ratelimit.RateLimitService;
import com.ecommerce.auth.ratelimit.exception.RateLimitExceededException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code /api/auth/signup}, {@code /api/auth/login} 앞단에서 IP 차단/요청수를 검사한다(US-401, US-403).
 * 일반 서블릿 필터다 — {@code SecurityFilterChain}에 속하지 않는다({@code RateLimitFilterConfig} 참고).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    // 이 필터 전용 ObjectMapper - 애플리케이션 전역 Bean에 기대지 않는다(간단한 오류 응답 직렬화만 필요).
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String endpoint = endpointNameFor(request.getRequestURI());

        if (rateLimitService.isIpBlocked(ip)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "IP_BLOCKED", "This IP is temporarily blocked.");
            return;
        }

        try {
            rateLimitService.assertIpWithinLimit(ip, endpoint);
        } catch (RateLimitExceededException ex) {
            writeError(
                    response,
                    429,
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Please try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String endpointNameFor(String requestUri) {
        int lastSlash = requestUri.lastIndexOf('/');
        return lastSlash >= 0 ? requestUri.substring(lastSlash + 1) : requestUri;
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("code", code, "message", message));
    }
}
