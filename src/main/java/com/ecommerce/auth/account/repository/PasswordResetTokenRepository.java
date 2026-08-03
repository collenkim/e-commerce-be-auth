package com.ecommerce.auth.account.repository;

import com.ecommerce.auth.account.domain.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findAllByAccountIdAndConsumedAtIsNull(UUID accountId);
}
