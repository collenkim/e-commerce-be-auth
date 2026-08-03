package com.ecommerce.auth.token.domain;

import com.ecommerce.auth.shared.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Refresh Token 레코드. 상태와 무관하게 삭제하지 않고 영구 보관한다 (functional-design/business-rules.md Q5:A).
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefreshTokenStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "previous_token_id")
    private UUID previousTokenId;

    private RefreshToken(
            UUID accountId,
            Role role,
            String tokenHash,
            UUID familyId,
            Instant issuedAt,
            Instant expiresAt,
            UUID previousTokenId) {
        this.accountId = accountId;
        this.role = role;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.status = RefreshTokenStatus.ACTIVE;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.previousTokenId = previousTokenId;
    }

    /** 최초 로그인 시 새 패밀리를 시작하는 Refresh Token을 생성한다. */
    public static RefreshToken issueNewFamily(
            UUID accountId, Role role, String tokenHash, Instant issuedAt, Instant expiresAt) {
        return new RefreshToken(accountId, role, tokenHash, UUID.randomUUID(), issuedAt, expiresAt, null);
    }

    /** 기존 패밀리를 승계해 회전된 Refresh Token을 생성한다. 역할(role)은 발급 시점 값을 그대로 승계한다. */
    public RefreshToken rotateToNew(String newTokenHash, Instant issuedAt, Instant expiresAt) {
        return new RefreshToken(
                this.accountId, this.role, newTokenHash, this.familyId, issuedAt, expiresAt, this.id);
    }

    public boolean isActive() {
        return status == RefreshTokenStatus.ACTIVE;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void markRotated() {
        this.status = RefreshTokenStatus.ROTATED;
    }

    public void markRevoked() {
        this.status = RefreshTokenStatus.REVOKED;
    }
}
