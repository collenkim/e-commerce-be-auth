package com.ecommerce.auth.account.domain;

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

/** 이메일/비밀번호 계정. domain-entities.md 참고. */
@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private Account(String email, String passwordHash, Instant createdAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = Role.USER;
        this.status = AccountStatus.PENDING_VERIFICATION;
        this.createdAt = createdAt;
    }

    /** 회원가입 시 새 계정을 생성한다. 항상 USER 역할, 미인증 상태로 시작한다 (FR-01). */
    public static Account signUp(String normalizedEmail, String passwordHash, Instant now) {
        return new Account(normalizedEmail, passwordHash, now);
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void activate(Instant now) {
        this.status = AccountStatus.ACTIVE;
        this.emailVerifiedAt = now;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /** 관리자에 의한 역할 변경 (US-502, Authorization Unit). */
    public void changeRole(Role newRole) {
        this.role = newRole;
    }
}
