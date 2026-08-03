package com.ecommerce.auth.token.repository;

import com.ecommerce.auth.token.domain.RefreshToken;
import com.ecommerce.auth.token.domain.RefreshTokenStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByFamilyId(UUID familyId);

    List<RefreshToken> findAllByAccountIdAndStatus(UUID accountId, RefreshTokenStatus status);
}
