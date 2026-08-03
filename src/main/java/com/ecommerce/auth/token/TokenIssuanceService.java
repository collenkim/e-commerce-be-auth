package com.ecommerce.auth.token;

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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Token Unit의 오케스트레이션 서비스. functional-design/business-logic-model.md의 6개 절차를 구현한다.
 * US-301, US-302, US-303, US-304.
 */
@Service
public class TokenIssuanceService {

    private static final Logger log = LoggerFactory.getLogger(TokenIssuanceService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistStore blacklistStore;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final Clock clock;

    public TokenIssuanceService(
            RefreshTokenRepository refreshTokenRepository,
            TokenBlacklistStore blacklistStore,
            JwtProvider jwtProvider,
            JwtProperties jwtProperties,
            OpaqueTokenGenerator opaqueTokenGenerator,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.blacklistStore = blacklistStore;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
        this.opaqueTokenGenerator = opaqueTokenGenerator;
        this.clock = clock;
    }

    /** 절차 1: 로그인 성공 시 새 토큰 패밀리를 시작하며 토큰 쌍을 발급한다. */
    @Transactional
    public TokenPair issue(UUID accountId, Role role) {
        Instant now = clock.instant();
        String refreshPlain = opaqueTokenGenerator.generate();
        RefreshToken saved =
                refreshTokenRepository.save(
                        RefreshToken.issueNewFamily(
                                accountId,
                                role,
                                opaqueTokenGenerator.hash(refreshPlain),
                                now,
                                now.plus(jwtProperties.refreshTokenTtl())));

        String accessToken = issueAccessToken(accountId, role, saved.getFamilyId(), now);
        return new TokenPair(accessToken, refreshPlain);
    }

    /**
     * 절차 2/3: Refresh Token 회전. 이미 사용된(ROTATED/REVOKED) 토큰이면 재사용으로 간주해 패밀리를 전부 무효화한다.
     * 역할(role)은 최초 발급 시점 값을 그대로 승계한다 — 세션 중 역할이 바뀌면 재로그인이 필요하다(알려진 제약).
     */
    @Transactional
    public TokenPair refresh(String refreshTokenPlain) {
        Instant now = clock.instant();
        RefreshToken existing =
                refreshTokenRepository
                        .findByTokenHash(opaqueTokenGenerator.hash(refreshTokenPlain))
                        .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!existing.isActive()) {
            revokeFamily(existing.getFamilyId());
            log.warn("Refresh token reuse detected, family revoked (familyId={})", existing.getFamilyId());
            throw new TokenReuseDetectedException("Refresh token reuse detected");
        }
        if (existing.isExpired(now)) {
            throw new InvalidTokenException("Refresh token expired");
        }

        existing.markRotated();
        refreshTokenRepository.save(existing);

        String newRefreshPlain = opaqueTokenGenerator.generate();
        RefreshToken rotated =
                refreshTokenRepository.save(
                        existing.rotateToNew(
                                opaqueTokenGenerator.hash(newRefreshPlain),
                                now,
                                now.plus(jwtProperties.refreshTokenTtl())));

        String accessToken =
                issueAccessToken(existing.getAccountId(), existing.getRole(), rotated.getFamilyId(), now);
        return new TokenPair(accessToken, newRefreshPlain);
    }

    /** 절차 4: 로그아웃 — Refresh Token 폐기 + Access Token jti 블랙리스트 등록. */
    @Transactional
    public void logout(String accessToken, String refreshTokenPlain) {
        refreshTokenRepository
                .findByTokenHash(opaqueTokenGenerator.hash(refreshTokenPlain))
                .filter(RefreshToken::isActive)
                .ifPresent(
                        token -> {
                            token.markRevoked();
                            refreshTokenRepository.save(token);
                        });

        AccessTokenClaims claims = jwtProvider.parse(accessToken);
        blacklistStore.register(claims.jti(), JwtProvider.remainingTtl(claims.expiresAt(), clock.instant()));
    }

    /** 절차 5: 토큰 검증/인트로스펙션 (FR-07, 외부 호출자용). */
    public TokenValidationResult validate(String accessToken) {
        AccessTokenClaims claims;
        try {
            claims = jwtProvider.parse(accessToken);
        } catch (InvalidTokenException ex) {
            return TokenValidationResult.invalid();
        }
        if (blacklistStore.isBlacklisted(claims.jti())) {
            return TokenValidationResult.invalid();
        }
        return TokenValidationResult.valid(claims.accountId(), claims.role());
    }

    /** 절차 6: 비밀번호 변경 시 계정의 모든 활성 Refresh Token을 무효화한다 (Account Unit 협력, US-104 지원). */
    @Transactional
    public void revokeAllForAccount(UUID accountId) {
        List<RefreshToken> active =
                refreshTokenRepository.findAllByAccountIdAndStatus(accountId, RefreshTokenStatus.ACTIVE);
        active.forEach(RefreshToken::markRevoked);
        refreshTokenRepository.saveAll(active);
    }

    private void revokeFamily(UUID familyId) {
        List<RefreshToken> family = refreshTokenRepository.findAllByFamilyId(familyId);
        family.stream().filter(RefreshToken::isActive).forEach(RefreshToken::markRevoked);
        refreshTokenRepository.saveAll(family);
    }

    private String issueAccessToken(UUID accountId, Role role, UUID familyId, Instant now) {
        AccessTokenClaims claims =
                new AccessTokenClaims(
                        accountId,
                        role,
                        familyId,
                        JwtProvider.newJti(),
                        now,
                        now.plus(jwtProperties.accessTokenTtl()));
        return jwtProvider.issue(claims);
    }
}
