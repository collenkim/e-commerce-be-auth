package com.ecommerce.auth.account.repository;

import com.ecommerce.auth.account.domain.EmailVerificationToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findAllByAccountIdAndConsumedAtIsNull(UUID accountId);
}
