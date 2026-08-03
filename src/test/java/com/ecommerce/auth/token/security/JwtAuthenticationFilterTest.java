package com.ecommerce.auth.token.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.dto.TokenValidationResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final TokenIssuanceService tokenIssuanceService = mock(TokenIssuanceService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenIssuanceService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerToken_populatesSecurityContext() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(tokenIssuanceService.validate("good-token"))
                .thenReturn(TokenValidationResult.valid(accountId, Role.ADMIN));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(accountId);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidToken_leavesSecurityContextEmptyButContinuesChain() throws Exception {
        when(tokenIssuanceService.validate("bad-token")).thenReturn(TokenValidationResult.invalid());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingAuthorizationHeader_continuesChainWithoutValidating() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
