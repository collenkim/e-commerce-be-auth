package com.ecommerce.auth.token.api.dto;

import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.dto.TokenValidationResult;
import java.util.UUID;

public record ValidateTokenResponse(boolean valid, UUID accountId, Role role) {

    public static ValidateTokenResponse from(TokenValidationResult result) {
        return new ValidateTokenResponse(result.valid(), result.accountId(), result.role());
    }
}
