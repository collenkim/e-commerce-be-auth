package com.ecommerce.auth.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** RateLimit Unit 설정값 (하드코딩 금지 — infrastructure-design.md 참고). */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        int ipLimitPerWindow,
        Duration ipWindow,
        int ipViolationThreshold,
        Duration ipViolationWindow,
        int accountFailureThreshold,
        Duration accountWindow,
        int bruteForceDistinctAccountThreshold,
        Duration bruteForceWindow,
        Duration blockDuration) {

    public RateLimitProperties {
        if (ipLimitPerWindow <= 0) {
            ipLimitPerWindow = 10;
        }
        if (ipWindow == null) {
            ipWindow = Duration.ofSeconds(60);
        }
        if (ipViolationThreshold <= 0) {
            ipViolationThreshold = 3;
        }
        if (ipViolationWindow == null) {
            ipViolationWindow = Duration.ofSeconds(60);
        }
        if (accountFailureThreshold <= 0) {
            accountFailureThreshold = 5;
        }
        if (accountWindow == null) {
            accountWindow = Duration.ofMinutes(10);
        }
        if (bruteForceDistinctAccountThreshold <= 0) {
            bruteForceDistinctAccountThreshold = 10;
        }
        if (bruteForceWindow == null) {
            bruteForceWindow = Duration.ofMinutes(5);
        }
        if (blockDuration == null) {
            blockDuration = Duration.ofMinutes(15);
        }
    }
}
