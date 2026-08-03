package com.ecommerce.auth.ratelimit;

import com.ecommerce.auth.ratelimit.exception.AccountBlockedException;
import com.ecommerce.auth.ratelimit.exception.RateLimitExceededException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * IP/계정 기준 Rate Limit 및 자동 차단. Redis 장애 시 fail-open(허용) — 인가 핵심 기능이 아닌 남용
 * 방지 기능이므로 가용성을 우선한다(nfr-requirements.md Q1:A, Token Unit의 fail-closed와 의도적으로 다름).
 * US-401~405.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimitService(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /** IP 차단 여부 확인. Redis 장애 시 차단되지 않은 것으로 간주(fail-open). */
    public boolean isIpBlocked(String ip) {
        return failOpen(() -> redisTemplate.hasKey(ipBlockKey(ip)), false, "isIpBlocked");
    }

    /**
     * IP 기준 요청 수를 증가시키고 한도를 검사한다(US-401). 초과 시 위반 카운터를 증가시키고
     * 임계치 도달 시 IP를 차단한다(US-403). Redis 장애 시 통과시킨다(fail-open).
     */
    public void assertIpWithinLimit(String ip, String endpoint) {
        try {
            long count = increment(ipCounterKey(endpoint, ip), properties.ipWindow());
            if (count > properties.ipLimitPerWindow()) {
                long violations = increment(ipViolationKey(ip), properties.ipViolationWindow());
                if (violations >= properties.ipViolationThreshold()) {
                    blockIp(ip, BlockReason.RATE_LIMIT);
                }
                throw new RateLimitExceededException("IP rate limit exceeded");
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable during rate limit check, failing open (ip={})", ip, ex);
        }
    }

    /** 계정 차단 여부 확인(US-402). Redis 장애 시 차단되지 않은 것으로 간주(fail-open). */
    public void assertAccountNotBlocked(String email) {
        boolean blocked = failOpen(() -> redisTemplate.hasKey(accountBlockKey(email)), false, "assertAccountNotBlocked");
        if (blocked) {
            throw new AccountBlockedException("Account temporarily locked due to repeated failed logins");
        }
    }

    /**
     * 로그인 실패를 기록한다 — 계정 기준 실패 카운트(US-402) + 브루트포스 대상 계정 추적(US-404).
     * Redis 장애 시 아무 것도 하지 않는다(fail-open).
     */
    public void recordLoginFailure(String ip, String email) {
        try {
            long accountFailures = increment(accountCounterKey(email), properties.accountWindow());
            if (accountFailures >= properties.accountFailureThreshold()) {
                redisTemplate.opsForValue().set(accountBlockKey(email), "blocked", properties.blockDuration());
            }

            String targetsKey = bruteForceTargetsKey(ip);
            redisTemplate.opsForSet().add(targetsKey, email);
            redisTemplate.expire(targetsKey, properties.bruteForceWindow());
            Long distinctTargets = redisTemplate.opsForSet().size(targetsKey);
            if (distinctTargets != null && distinctTargets >= properties.bruteForceDistinctAccountThreshold()) {
                blockIp(ip, BlockReason.BRUTE_FORCE);
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while recording login failure, failing open (ip={})", ip, ex);
        }
    }

    private void blockIp(String ip, BlockReason reason) {
        redisTemplate.opsForValue().set(ipBlockKey(ip), reason.name(), properties.blockDuration());
        log.warn("IP blocked (ip={}, reason={}, duration={})", ip, reason, properties.blockDuration());
    }

    private long increment(String key, Duration window) {
        Long value = redisTemplate.opsForValue().increment(key);
        long count = value == null ? 1L : value;
        if (count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count;
    }

    private boolean failOpen(RedisBooleanOperation operation, boolean defaultValue, String operationName) {
        try {
            Boolean result = operation.execute();
            return result != null && result;
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable during '{}', failing open", operationName, ex);
            return defaultValue;
        }
    }

    private static String ipCounterKey(String endpoint, String ip) {
        return "ratelimit:ip:" + endpoint + ":" + ip;
    }

    private static String ipViolationKey(String ip) {
        return "ratelimit:ip-violation:" + ip;
    }

    private static String accountCounterKey(String email) {
        return "ratelimit:account:" + email;
    }

    private static String ipBlockKey(String ip) {
        return "block:ip:" + ip;
    }

    private static String accountBlockKey(String email) {
        return "block:account:" + email;
    }

    private static String bruteForceTargetsKey(String ip) {
        return "bruteforce:ip-targets:" + ip;
    }

    @FunctionalInterface
    private interface RedisBooleanOperation {
        Boolean execute();
    }
}
