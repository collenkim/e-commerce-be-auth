package com.ecommerce.auth.token.api;

import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.api.dto.RefreshTokenRequest;
import com.ecommerce.auth.token.api.dto.TokenPairResponse;
import com.ecommerce.auth.token.exception.InvalidTokenException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** US-302(갱신/회전), US-304(로그아웃) 공개 API. */
@RestController
public class TokenController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenIssuanceService tokenIssuanceService;

    public TokenController(TokenIssuanceService tokenIssuanceService) {
        this.tokenIssuanceService = tokenIssuanceService;
    }

    @PostMapping("/api/auth/token/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenPairResponse.from(tokenIssuanceService.refresh(request.refreshToken()));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody RefreshTokenRequest request) {
        String accessToken = extractBearerToken(authorizationHeader);
        tokenIssuanceService.logout(accessToken, request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException("Missing or malformed Authorization header");
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
