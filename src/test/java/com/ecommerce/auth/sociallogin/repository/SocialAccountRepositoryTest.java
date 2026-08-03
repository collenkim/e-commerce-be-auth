package com.ecommerce.auth.sociallogin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.auth.sociallogin.domain.SocialAccount;
import com.ecommerce.auth.sociallogin.domain.SocialProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class SocialAccountRepositoryTest {

    @Autowired private SocialAccountRepository repository;

    @Test
    void findByProviderAndProviderUserId_returnsSavedLink() {
        UUID accountId = UUID.randomUUID();
        SocialAccount saved =
                repository.save(new SocialAccount(SocialProvider.GOOGLE, "g-1", accountId, Instant.now()));

        assertThat(repository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "g-1")).contains(saved);
        assertThat(repository.findByProviderAndProviderUserId(SocialProvider.KAKAO, "g-1")).isEmpty();
    }
}
