package com.ecommerce.auth.sociallogin.security;

import com.ecommerce.auth.sociallogin.NormalizingOAuth2UserService;
import com.ecommerce.auth.sociallogin.SocialLoginProperties;
import com.ecommerce.auth.sociallogin.SocialLoginService;
import com.ecommerce.auth.sociallogin.domain.SocialProvider;
import com.ecommerce.auth.sociallogin.dto.NormalizedSocialUser;
import com.ecommerce.auth.sociallogin.dto.SocialLoginOutcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * OAuth2 로그인 성공 시 세션 기반 인증 완료 대신 JWT를 발급하고 프런트엔드로 리다이렉트한다
 * (nfr-design-patterns.md: 세션 무상태화 패턴).
 */
@Component
public class SocialLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final SocialLoginProperties properties;

    public SocialLoginSuccessHandler(SocialLoginService socialLoginService, SocialLoginProperties properties) {
        this.socialLoginService = socialLoginService;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();
        SocialProvider provider = SocialProvider.fromRegistrationId(oauthToken.getAuthorizedClientRegistrationId());

        NormalizedSocialUser user =
                new NormalizedSocialUser(
                        provider,
                        (String) oauth2User.getAttributes().get(NormalizingOAuth2UserService.PROVIDER_USER_ID_KEY),
                        (String) oauth2User.getAttributes().get(NormalizingOAuth2UserService.EMAIL_KEY));

        SocialLoginOutcome outcome = socialLoginService.handleLogin(user);

        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(buildRedirectUrl(outcome));
    }

    private String buildRedirectUrl(SocialLoginOutcome outcome) {
        String base = properties.frontendRedirectUri();
        if (outcome.linkRequired()) {
            return base + "#linkRequired=true&linkToken=" + encode(outcome.linkTokenPlain());
        }
        return base
                + "#access_token="
                + encode(outcome.tokens().accessToken())
                + "&refresh_token="
                + encode(outcome.tokens().refreshToken());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
