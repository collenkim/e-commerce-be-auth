package com.ecommerce.auth.sociallogin.security;

import com.ecommerce.auth.sociallogin.SocialLoginProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** 실패 시 재시도 없이 일반화된 오류로 프런트엔드에 리다이렉트한다 (SECURITY-09, 최소 회복탄력성 방침). */
@Component
public class SocialLoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginFailureHandler.class);

    private final SocialLoginProperties properties;

    public SocialLoginFailureHandler(SocialLoginProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        log.warn("Social login failed", exception);
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(properties.frontendRedirectUri() + "#error=social_login_failed");
    }
}
