package com.ecommerce.auth.authorization.api.dto;

import com.ecommerce.auth.shared.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {}
