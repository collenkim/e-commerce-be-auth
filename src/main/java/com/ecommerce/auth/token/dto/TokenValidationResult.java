package com.ecommerce.auth.token.dto;

import com.ecommerce.auth.shared.Role;
import java.util.UUID;

/**
 * 토큰 검증/인트로스펙션 결과 (FR-07 — 외부 게이트웨이/다른 백엔드 서비스 호출용).
 * {@code valid=false}일 때 {@code accountId}/{@code role}은 null이다.
 */
public record TokenValidationResult(boolean valid, UUID accountId, Role role) {

    public static TokenValidationResult invalid() {
        return new TokenValidationResult(false, null, null);
    }

    public static TokenValidationResult valid(UUID accountId, Role role) {
        return new TokenValidationResult(true, accountId, role);
    }
}
