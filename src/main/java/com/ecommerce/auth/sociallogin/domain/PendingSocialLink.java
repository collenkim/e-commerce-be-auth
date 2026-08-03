package com.ecommerce.auth.sociallogin.domain;

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

/** 동일 이메일 기존 계정 발견 시, 사용자 확인 전까지 대기하는 연동 요청. TTL 10분. */
@Entity
@Table(name = "pending_social_link")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingSocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "existing_account_id", nullable = false)
    private UUID existingAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    public PendingSocialLink(
            UUID existingAccountId,
            SocialProvider provider,
            String providerUserId,
            String email,
            String tokenHash,
            Instant expiresAt) {
        this.existingAccountId = existingAccountId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
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
