package com.ecommerce.auth.sociallogin.repository;

import com.ecommerce.auth.sociallogin.domain.PendingSocialLink;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingSocialLinkRepository extends JpaRepository<PendingSocialLink, UUID> {

    Optional<PendingSocialLink> findByTokenHash(String tokenHash);
}
