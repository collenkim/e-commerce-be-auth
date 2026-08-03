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

/** provider 소셜 아이덴티티와 Account 간 연결. domain-entities.md 참고. */
@Entity
@Table(name = "social_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    public SocialAccount(SocialProvider provider, String providerUserId, UUID accountId, Instant linkedAt) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.accountId = accountId;
        this.linkedAt = linkedAt;
    }
}
