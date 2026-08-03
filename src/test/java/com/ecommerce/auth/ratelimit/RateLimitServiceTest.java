package com.ecommerce.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.auth.ratelimit.exception.AccountBlockedException;
import com.ecommerce.auth.ratelimit.exception.RateLimitExceededException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RateLimitServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SetOperations<String, String> setOperations;
    private RateLimitService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        RateLimitProperties properties =
                new RateLimitProperties(
                        3, Duration.ofSeconds(60), 2, Duration.ofSeconds(60), 2, Duration.ofMinutes(10), 2,
                        Duration.ofMinutes(5), Duration.ofMinutes(15));
        service = new RateLimitService(redisTemplate, properties);
    }

    @Test
    void isIpBlocked_true() {
        when(redisTemplate.hasKey("block:ip:1.2.3.4")).thenReturn(true);
        assertThat(service.isIpBlocked("1.2.3.4")).isTrue();
    }

    @Test
    void isIpBlocked_redisFails_failsOpenToFalse() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new QueryTimeoutException("down"));
        assertThat(service.isIpBlocked("1.2.3.4")).isFalse();
    }

    @Test
    void assertIpWithinLimit_underThreshold_doesNotThrow() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        assertThatCode(() -> service.assertIpWithinLimit("1.2.3.4", "login")).doesNotThrowAnyException();
    }

    @Test
    void assertIpWithinLimit_overThreshold_throwsAndDoesNotYetBlock() {
        when(valueOperations.increment("ratelimit:ip:login:1.2.3.4")).thenReturn(4L);
        when(valueOperations.increment("ratelimit:ip-violation:1.2.3.4")).thenReturn(1L);

        assertThatThrownBy(() -> service.assertIpWithinLimit("1.2.3.4", "login"))
                .isInstanceOf(RateLimitExceededException.class);
        verify(valueOperations, never()).set(eq("block:ip:1.2.3.4"), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void assertIpWithinLimit_violationThresholdReached_blocksIp() {
        when(valueOperations.increment("ratelimit:ip:login:1.2.3.4")).thenReturn(4L);
        when(valueOperations.increment("ratelimit:ip-violation:1.2.3.4")).thenReturn(2L);

        assertThatThrownBy(() -> service.assertIpWithinLimit("1.2.3.4", "login"))
                .isInstanceOf(RateLimitExceededException.class);
        verify(valueOperations)
                .set(eq("block:ip:1.2.3.4"), eq("RATE_LIMIT"), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void assertIpWithinLimit_redisFails_failsOpenSilently() {
        when(valueOperations.increment(anyString())).thenThrow(new QueryTimeoutException("down"));
        assertThatCode(() -> service.assertIpWithinLimit("1.2.3.4", "login")).doesNotThrowAnyException();
    }

    @Test
    void assertAccountNotBlocked_blocked_throws() {
        when(redisTemplate.hasKey("block:account:user@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.assertAccountNotBlocked("user@example.com"))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    void assertAccountNotBlocked_notBlocked_doesNotThrow() {
        when(redisTemplate.hasKey("block:account:user@example.com")).thenReturn(false);
        assertThatCode(() -> service.assertAccountNotBlocked("user@example.com")).doesNotThrowAnyException();
    }

    @Test
    void assertAccountNotBlocked_redisFails_failsOpen() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new QueryTimeoutException("down"));
        assertThatCode(() -> service.assertAccountNotBlocked("user@example.com")).doesNotThrowAnyException();
    }

    @Test
    void recordLoginFailure_accountThresholdReached_blocksAccount() {
        when(valueOperations.increment("ratelimit:account:user@example.com")).thenReturn(2L);
        when(setOperations.size(anyString())).thenReturn(1L);

        service.recordLoginFailure("1.2.3.4", "user@example.com");

        verify(valueOperations)
                .set(
                        eq("block:account:user@example.com"),
                        anyString(),
                        org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void recordLoginFailure_distinctAccountThresholdReached_blocksIp() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(setOperations.size("bruteforce:ip-targets:1.2.3.4")).thenReturn(2L);

        service.recordLoginFailure("1.2.3.4", "user@example.com");

        verify(setOperations).add("bruteforce:ip-targets:1.2.3.4", "user@example.com");
        verify(valueOperations)
                .set(eq("block:ip:1.2.3.4"), eq("BRUTE_FORCE"), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void recordLoginFailure_redisFails_doesNotThrow() {
        when(valueOperations.increment(anyString())).thenThrow(new QueryTimeoutException("down"));
        assertThatCode(() -> service.recordLoginFailure("1.2.3.4", "user@example.com"))
                .doesNotThrowAnyException();
    }
}
