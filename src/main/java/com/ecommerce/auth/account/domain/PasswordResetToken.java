package com.ecommerce.auth.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 비밀번호 재설정 토큰 (US-104). TTL 30분(Functional Design Q2:A). */
@Entity
@Table(name = "password_reset_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    public PasswordResetToken(UUID accountId, String tokenHash, Instant expiresAt) {
        this.accountId = accountId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now) {
        return consumedAt == null && now.isBefore(expiresAt);
    }

    public void markConsumed(Instant now) {
        this.consumedAt = now;
    }
}
