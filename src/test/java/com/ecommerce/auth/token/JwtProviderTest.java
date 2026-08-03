package com.ecommerce.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.dto.AccessTokenClaims;
import com.ecommerce.auth.token.exception.InvalidTokenException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final JwtProperties PROPERTIES =
            new JwtProperties("test-secret-key-must-be-at-least-32-bytes-long", null, null);
    private final JwtProvider jwtProvider = new JwtProvider(PROPERTIES);

    @Test
    void issueThenParse_returnsSameClaims() {
        Instant now = Instant.now();
        AccessTokenClaims original =
                new AccessTokenClaims(
                        UUID.randomUUID(),
                        Role.SELLER,
                        UUID.randomUUID(),
                        JwtProvider.newJti(),
                        now,
                        now.plus(Duration.ofMinutes(15)));

        String token = jwtProvider.issue(original);
        AccessTokenClaims parsed = jwtProvider.parse(token);

        assertThat(parsed.accountId()).isEqualTo(original.accountId());
        assertThat(parsed.role()).isEqualTo(original.role());
        assertThat(parsed.familyId()).isEqualTo(original.familyId());
        assertThat(parsed.jti()).isEqualTo(original.jti());
        // JWT NumericDate(iat/exp)는 초 단위 정밀도다(RFC 7519) - 나노초는 손실되는 것이 정상이다 (PBT-02 허용 오차).
        assertThat(parsed.issuedAt()).isEqualTo(original.issuedAt().truncatedTo(ChronoUnit.SECONDS));
        assertThat(parsed.expiresAt()).isEqualTo(original.expiresAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void parse_expiredToken_throwsInvalidTokenException() {
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        AccessTokenClaims expired =
                new AccessTokenClaims(
                        UUID.randomUUID(),
                        Role.USER,
                        UUID.randomUUID(),
                        JwtProvider.newJti(),
                        past,
                        past.plusSeconds(1));

        String token = jwtProvider.issue(expired);

        assertThatThrownBy(() -> jwtProvider.parse(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parse_tamperedToken_throwsInvalidTokenException() {
        Instant now = Instant.now();
        String token =
                jwtProvider.issue(
                        new AccessTokenClaims(
                                UUID.randomUUID(),
                                Role.USER,
                                UUID.randomUUID(),
                                JwtProvider.newJti(),
                                now,
                                now.plusSeconds(60)));
        // 마지막 base64url 문자는 패딩 비트가 있어 특정 값끼리는 디코딩 결과가 같을 수 있으므로(false negative 방지),
        // 서명 구간 중간의 문자를 뒤집어 확실히 다른 바이트로 디코딩되게 한다.
        int tamperIndex = token.length() - 6;
        char original = token.charAt(tamperIndex);
        char replacement = original == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, tamperIndex) + replacement + token.substring(tamperIndex + 1);

        assertThatThrownBy(() -> jwtProvider.parse(tampered)).isInstanceOf(InvalidTokenException.class);
    }
}
