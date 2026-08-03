package com.ecommerce.auth.account;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Account Unit 설정값 (하드코딩 금지 — infrastructure-design.md 참고). */
@ConfigurationProperties(prefix = "app.account")
public record AccountProperties(
        Duration emailVerificationTokenTtl, Duration passwordResetTokenTtl) {

    public AccountProperties {
        if (emailVerificationTokenTtl == null) {
            emailVerificationTokenTtl = Duration.ofHours(24);
        }
        if (passwordResetTokenTtl == null) {
            passwordResetTokenTtl = Duration.ofMinutes(30);
        }
    }
}
