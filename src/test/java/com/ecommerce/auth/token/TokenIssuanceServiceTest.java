package com.ecommerce.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.auth.shared.OpaqueTokenGenerator;
import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.domain.RefreshToken;
import com.ecommerce.auth.token.domain.RefreshTokenStatus;
import com.ecommerce.auth.token.dto.AccessTokenClaims;
import com.ecommerce.auth.token.dto.TokenPair;
import com.ecommerce.auth.token.dto.TokenValidationResult;
import com.ecommerce.auth.token.exception.InvalidTokenException;
import com.ecommerce.auth.token.exception.TokenReuseDetectedException;
import com.ecommerce.auth.token.repository.RefreshTokenRepository;
import com.ecommerce.auth.token.repository.TokenBlacklistStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenIssuanceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private RefreshTokenRepository repository;
    private TokenBlacklistStore blacklistStore;
    private JwtProvider jwtProvider;
    private TokenIssuanceService service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        blacklistStore = mock(TokenBlacklistStore.class);
        jwtProvider = mock(JwtProvider.class);
        JwtProperties properties =
                new JwtProperties("unused-in-mock", Duration.ofMinutes(15), Duration.ofDays(14));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service =
                new TokenIssuanceService(
                        repository, blacklistStore, jwtProvider, properties, new OpaqueTokenGenerator(), clock);

        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtProvider.issue(any(AccessTokenClaims.class))).thenReturn("signed.jwt.token");
    }

    @Test
    void issue_savesNewFamilyAndReturnsTokenPair() {
        UUID accountId = UUID.randomUUID();

        TokenPair result = service.issue(accountId, Role.USER);

        assertThat(result.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(result.refreshToken()).isNotBlank();
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_activeToken_rotatesAndReturnsNewPair() {
        RefreshToken existing =
                RefreshToken.issueNewFamily(
                        UUID.randomUUID(), Role.SELLER, "hash", NOW.minusSeconds(60), NOW.plusSeconds(60));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        TokenPair result = service.refresh("original-refresh-token-plain");

        assertThat(existing.getStatus()).isEqualTo(RefreshTokenStatus.ROTATED);
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.accessToken()).isEqualTo("signed.jwt.token");
        verify(repository, times(2)).save(any(RefreshToken.class)); // 기존(ROTATED 표시) + 신규
    }

    @Test
    void refresh_reusedRotatedToken_revokesFamilyAndThrows() {
        RefreshToken original =
                RefreshToken.issueNewFamily(
                        UUID.randomUUID(), Role.USER, "hash-1", NOW.minusSeconds(120), NOW.plusSeconds(600));
        RefreshToken rotated = original.rotateToNew("hash-2", NOW.minusSeconds(60), NOW.plusSeconds(600));
        original.markRotated(); // 이미 회전되어 더 이상 ACTIVE가 아닌 상태
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(original));
        when(repository.findAllByFamilyId(original.getFamilyId())).thenReturn(List.of(original, rotated));

        assertThatThrownBy(() -> service.refresh("already-rotated-token"))
                .isInstanceOf(TokenReuseDetectedException.class);

        assertThat(rotated.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);
        verify(repository).saveAll(List.of(original, rotated));
    }

    @Test
    void refresh_unknownToken_throwsInvalidTokenException() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("unknown")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_expiredActiveToken_throwsInvalidTokenExceptionWithoutRevokingFamily() {
        RefreshToken expired =
                RefreshToken.issueNewFamily(
                        UUID.randomUUID(), Role.USER, "hash", NOW.minusSeconds(600), NOW.minusSeconds(1));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh("expired")).isInstanceOf(InvalidTokenException.class);
        verify(repository, never()).findAllByFamilyId(any());
    }

    @Test
    void logout_revokesRefreshTokenAndBlacklistsAccessToken() {
        RefreshToken active =
                RefreshToken.issueNewFamily(
                        UUID.randomUUID(), Role.USER, "hash", NOW.minusSeconds(60), NOW.plusSeconds(600));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(active));
        AccessTokenClaims claims =
                new AccessTokenClaims(
                        UUID.randomUUID(),
                        Role.USER,
                        active.getFamilyId(),
                        "jti-123",
                        NOW.minusSeconds(30),
                        NOW.plusSeconds(300));
        when(jwtProvider.parse("access-token")).thenReturn(claims);

        service.logout("access-token", "refresh-token-plain");

        assertThat(active.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);
        verify(blacklistStore).register("jti-123", Duration.ofSeconds(300));
    }

    @Test
    void validate_validNonBlacklistedToken_returnsValidResult() {
        UUID accountId = UUID.randomUUID();
        AccessTokenClaims claims =
                new AccessTokenClaims(
                        accountId, Role.ADMIN, UUID.randomUUID(), "jti-1", NOW, NOW.plusSeconds(60));
        when(jwtProvider.parse("token")).thenReturn(claims);
        when(blacklistStore.isBlacklisted("jti-1")).thenReturn(false);

        TokenValidationResult result = service.validate("token");

        assertThat(result.valid()).isTrue();
        assertThat(result.accountId()).isEqualTo(accountId);
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void validate_blacklistedToken_returnsInvalidResult() {
        AccessTokenClaims claims =
                new AccessTokenClaims(
                        UUID.randomUUID(), Role.USER, UUID.randomUUID(), "jti-2", NOW, NOW.plusSeconds(60));
        when(jwtProvider.parse("token")).thenReturn(claims);
        when(blacklistStore.isBlacklisted("jti-2")).thenReturn(true);

        assertThat(service.validate("token").valid()).isFalse();
    }

    @Test
    void validate_malformedToken_returnsInvalidResultWithoutThrowing() {
        when(jwtProvider.parse("bad-token")).thenThrow(new InvalidTokenException("bad signature"));

        assertThat(service.validate("bad-token").valid()).isFalse();
    }

    @Test
    void revokeAllForAccount_revokesAllActiveTokens() {
        UUID accountId = UUID.randomUUID();
        RefreshToken t1 = RefreshToken.issueNewFamily(accountId, Role.USER, "h1", NOW, NOW.plusSeconds(600));
        RefreshToken t2 = RefreshToken.issueNewFamily(accountId, Role.USER, "h2", NOW, NOW.plusSeconds(600));
        when(repository.findAllByAccountIdAndStatus(accountId, RefreshTokenStatus.ACTIVE))
                .thenReturn(List.of(t1, t2));

        service.revokeAllForAccount(accountId);

        assertThat(t1.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);
        assertThat(t2.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);
    }
}
