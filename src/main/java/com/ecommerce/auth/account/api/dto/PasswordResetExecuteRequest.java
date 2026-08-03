package com.ecommerce.auth.account.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetExecuteRequest(
        @NotBlank @Size(max = 512) String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
