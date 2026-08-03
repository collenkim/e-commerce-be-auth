package com.ecommerce.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.dto.AccessTokenClaims;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * PBT-02 (Round-Trip Properties, Partial 모드 강제 대상): JWT 클레임 encode/decode는
 * f_inverse(f(x)) == x를 만족해야 한다. 도메인 타입 전용 생성기 사용 (PBT-07).
 */
class JwtProviderPropertyTest {

    private static final JwtProperties PROPERTIES =
            new JwtProperties("test-secret-key-must-be-at-least-32-bytes-long", null, null);
    private final JwtProvider jwtProvider = new JwtProvider(PROPERTIES);

    @Property
    void issueThenParse_isIdentity_forAnyValidClaims(@ForAll("accessTokenClaims") AccessTokenClaims claims) {
        String token = jwtProvider.issue(claims);
        AccessTokenClaims roundTripped = jwtProvider.parse(token);

        assertThat(roundTripped.accountId()).isEqualTo(claims.accountId());
        assertThat(roundTripped.role()).isEqualTo(claims.role());
        assertThat(roundTripped.familyId()).isEqualTo(claims.familyId());
        assertThat(roundTripped.jti()).isEqualTo(claims.jti());
        // JWT NumericDate(iat/exp)는 초 단위 정밀도다(RFC 7519) - 나노초는 손실되는 것이 정상이다 (PBT-02 허용 오차).
        assertThat(roundTripped.issuedAt()).isEqualTo(claims.issuedAt().truncatedTo(ChronoUnit.SECONDS));
        assertThat(roundTripped.expiresAt()).isEqualTo(claims.expiresAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Provide
    Arbitrary<AccessTokenClaims> accessTokenClaims() {
        Arbitrary<UUID> uuids = Arbitraries.create(UUID::randomUUID);
        Arbitrary<Role> roles = Arbitraries.of(Role.values());
        // jjwt는 실제 시스템 시계 기준으로 만료를 검증하므로(exp 필드 자체의 encode/decode와는 별개 관심사),
        // 발급시각은 "지금"에서 최대 5분 전까지로 제한해 만료시각(발급+15분)이 항상 실제 미래가 되도록 한다 (도메인 제약, PBT-07).
        Instant now = Instant.now();
        Arbitrary<Instant> issuedAtArb =
                Arbitraries.longs().between(0L, 300L).map(secondsAgo -> now.minusSeconds(secondsAgo));

        return net.jqwik.api.Combinators.combine(uuids, roles, uuids, issuedAtArb)
                .as(
                        (accountId, role, familyId, issuedAt) ->
                                new AccessTokenClaims(
                                        accountId,
                                        role,
                                        familyId,
                                        UUID.randomUUID().toString(),
                                        issuedAt,
                                        issuedAt.plus(Duration.ofMinutes(15))));
    }
}
