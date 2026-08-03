package com.ecommerce.auth.account.event;

import java.util.UUID;

public record EmailVerificationRequested(UUID accountId, String email, String verificationTokenPlain) {}
