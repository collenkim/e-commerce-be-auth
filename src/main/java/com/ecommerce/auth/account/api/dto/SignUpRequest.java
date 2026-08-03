package com.ecommerce.auth.account.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(min = 8, max = 128) String password) {}
