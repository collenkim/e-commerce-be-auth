package com.ecommerce.auth.token.dto;

import com.ecommerce.auth.shared.Role;
import java.time.Instant;
import java.util.UUID;

/** Access Token(JWT)이 담는 클레임. domain-entities.md 클레임 표 참고. */
public record AccessTokenClaims(
        UUID accountId,
        Role role,
        UUID familyId,
        String jti,
        Instant issuedAt,
        Instant expiresAt) {}
