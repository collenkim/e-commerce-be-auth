package com.ecommerce.auth.ratelimit.filter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.auth.ratelimit.RateLimitService;
import com.ecommerce.auth.ratelimit.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class RateLimitFilterTest {

    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final RateLimitFilter filter = new RateLimitFilter(rateLimitService);

    @Test
    void ipBlocked_writes403AndDoesNotContinueChain() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(rateLimitService.isIpBlocked("1.2.3.4")).thenReturn(true);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void ipOverLimit_writes429AndDoesNotContinueChain() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(rateLimitService.isIpBlocked("1.2.3.4")).thenReturn(false);
        doThrow(new RateLimitExceededException("too many"))
                .when(rateLimitService)
                .assertIpWithinLimit("1.2.3.4", "login");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        verify(response).setStatus(429);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void ipAllowed_continuesChain() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getRequestURI()).thenReturn("/api/auth/signup");
        when(rateLimitService.isIpBlocked(anyString())).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
