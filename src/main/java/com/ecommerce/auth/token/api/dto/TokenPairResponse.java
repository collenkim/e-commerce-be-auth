package com.ecommerce.auth.token.api.dto;

import com.ecommerce.auth.token.dto.TokenPair;

public record TokenPairResponse(String accessToken, String refreshToken) {

    public static TokenPairResponse from(TokenPair pair) {
        return new TokenPairResponse(pair.accessToken(), pair.refreshToken());
    }
}
