package com.ecommerce.auth.sociallogin;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** SocialLogin Unit 설정값 (하드코딩 금지 — infrastructure-design.md 참고). */
@ConfigurationProperties(prefix = "app.social-login")
public record SocialLoginProperties(Duration pendingLinkTtl, String frontendRedirectUri) {

    public SocialLoginProperties {
        if (pendingLinkTtl == null) {
            pendingLinkTtl = Duration.ofMinutes(10);
        }
        if (frontendRedirectUri == null) {
            frontendRedirectUri = "http://localhost:3000/oauth2/callback";
        }
    }
}
