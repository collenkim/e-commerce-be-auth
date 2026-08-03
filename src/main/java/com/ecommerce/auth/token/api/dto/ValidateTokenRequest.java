package com.ecommerce.auth.token.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidateTokenRequest(@NotBlank @Size(max = 2048) String accessToken) {}
