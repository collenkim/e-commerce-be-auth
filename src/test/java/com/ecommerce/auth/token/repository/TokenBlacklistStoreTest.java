package com.ecommerce.auth.token.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TokenBlacklistStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TokenBlacklistStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new TokenBlacklistStore(redisTemplate);
    }

    @Test
    void isBlacklisted_redisHealthy_returnsResultWithoutRetry() {
        when(redisTemplate.hasKey("blacklist:accessToken:jti-1")).thenReturn(true);

        assertThat(store.isBlacklisted("jti-1")).isTrue();
        verify(redisTemplate, times(1)).hasKey("blacklist:accessToken:jti-1");
    }

    @Test
    void isBlacklisted_transientFailureThenSuccess_retriesOnceAndSucceeds() {
        when(redisTemplate.hasKey("blacklist:accessToken:jti-2"))
                .thenThrow(new QueryTimeoutException("timeout"))
                .thenReturn(false);

        assertThat(store.isBlacklisted("jti-2")).isFalse();
        verify(redisTemplate, times(2)).hasKey("blacklist:accessToken:jti-2");
    }

    @Test
    void isBlacklisted_failsAfterRetry_failsClosed() {
        when(redisTemplate.hasKey("blacklist:accessToken:jti-3"))
                .thenThrow(new QueryTimeoutException("timeout 1"))
                .thenThrow(new QueryTimeoutException("timeout 2"));

        assertThat(store.isBlacklisted("jti-3")).isTrue(); // fail-closed: 차단된 것으로 간주
        verify(redisTemplate, times(2)).hasKey("blacklist:accessToken:jti-3");
    }

    @Test
    void register_setsKeyWithRemainingTtl() {
        store.register("jti-4", Duration.ofMinutes(5));

        verify(valueOperations).set("blacklist:accessToken:jti-4", "revoked", Duration.ofMinutes(5));
    }

    @Test
    void register_zeroOrNegativeTtl_skipsRedisCall() {
        store.register("jti-5", Duration.ZERO);
        store.register("jti-6", Duration.ofSeconds(-1));

        verify(valueOperations, times(0)).set(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
    }
}
