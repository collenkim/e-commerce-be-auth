package com.ecommerce.auth.sociallogin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkConfirmRequest(
        @NotBlank @Size(max = 512) String linkToken, @NotBlank @Size(max = 128) String existingPassword) {}
