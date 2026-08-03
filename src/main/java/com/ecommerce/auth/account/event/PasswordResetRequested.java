package com.ecommerce.auth.account.event;

import java.util.UUID;

public record PasswordResetRequested(UUID accountId, String email, String resetTokenPlain) {}
