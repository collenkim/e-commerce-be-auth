package com.ecommerce.auth.sociallogin.repository;

import com.ecommerce.auth.sociallogin.domain.SocialAccount;
import com.ecommerce.auth.sociallogin.domain.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
