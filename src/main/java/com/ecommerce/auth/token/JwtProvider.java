package com.ecommerce.auth.token;

import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.dto.AccessTokenClaims;
import com.ecommerce.auth.token.exception.InvalidTokenException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Access Token(JWT) 서명/파싱. HS256, 대칭키 (functional-design Q2:A). */
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_FAMILY_ID = "familyId";

    private final SecretKey signingKey;

    public JwtProvider(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(AccessTokenClaims claims) {
        return Jwts.builder()
                .subject(claims.accountId().toString())
                .id(claims.jti())
                .claim(CLAIM_ROLE, claims.role().name())
                .claim(CLAIM_FAMILY_ID, claims.familyId().toString())
                .issuedAt(Date.from(claims.issuedAt()))
                .expiration(Date.from(claims.expiresAt()))
                .signWith(signingKey)
                .compact();
    }

    /** 서명과 만료를 검증하고 클레임을 반환한다. 서명이 무효하거나 만료됐으면 {@link InvalidTokenException}. */
    public AccessTokenClaims parse(String token) {
        try {
            var jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            var body = jws.getPayload();
            return new AccessTokenClaims(
                    UUID.fromString(body.getSubject()),
                    Role.valueOf(body.get(CLAIM_ROLE, String.class)),
                    UUID.fromString(body.get(CLAIM_FAMILY_ID, String.class)),
                    body.getId(),
                    body.getIssuedAt().toInstant(),
                    body.getExpiration().toInstant());
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("Access token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Access token invalid: " + ex.getMessage());
        }
    }

    public static String newJti() {
        return UUID.randomUUID().toString();
    }

    /** 테스트/서비스 계층 편의를 위해 현재 시각 기준 만료까지 남은 시간을 계산한다. */
    public static java.time.Duration remainingTtl(Instant expiresAt, Instant now) {
        var remaining = java.time.Duration.between(now, expiresAt);
        return remaining.isNegative() ? java.time.Duration.ZERO : remaining;
    }
}
