package com.ecommerce.auth.token;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token Unit 설정값. 하드코딩 금지 — infrastructure-design.md 참고.
 * {@code app.jwt.secret}은 환경변수(JWT_HMAC_SECRET)로 주입되며 소스/설정파일에 커밋되지 않는다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {

    public JwtProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(14);
        }
    }
}
