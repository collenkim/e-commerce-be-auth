package com.ecommerce.auth.token.repository;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Access Token 블랙리스트 (Redis, jti 키). Redis 접근 실패 시 1회 재시도 후 fail-closed
 * (nfr-design/nfr-design-patterns.md).
 */
@Repository
public class TokenBlacklistStore {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistStore.class);
    private static final String KEY_PREFIX = "blacklist:accessToken:";
    private static final String BLACKLISTED_VALUE = "revoked";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 로그아웃 시 Access Token의 jti를 남은 만료 시간만큼 블랙리스트에 등록한다. 실패 시 예외를 던진다(fail-closed). */
    public void register(String jti, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            return; // 이미 만료된 토큰은 등록할 필요 없음
        }
        withRetry(
                () -> {
                    redisTemplate.opsForValue().set(KEY_PREFIX + jti, BLACKLISTED_VALUE, ttl);
                    return null;
                },
                "register");
    }

    /** jti가 블랙리스트에 있는지 조회한다. Redis 접근이 재시도 후에도 실패하면 fail-closed로 true(차단됨)를 반환한다. */
    public boolean isBlacklisted(String jti) {
        try {
            return withRetry(() -> redisTemplate.hasKey(KEY_PREFIX + jti), "isBlacklisted");
        } catch (DataAccessException ex) {
            log.warn("Redis blacklist check failed after retry, failing closed (jti={})", jti);
            return true;
        }
    }

    private <T> T withRetry(RedisOperation<T> operation, String operationName) {
        try {
            return operation.execute();
        } catch (DataAccessException firstFailure) {
            log.warn("Redis operation '{}' failed, retrying once", operationName, firstFailure);
            try {
                return operation.execute();
            } catch (DataAccessException secondFailure) {
                log.error("Redis operation '{}' failed after retry", operationName, secondFailure);
                throw secondFailure;
            }
        }
    }

    @FunctionalInterface
    private interface RedisOperation<T> {
        T execute();
    }
}
