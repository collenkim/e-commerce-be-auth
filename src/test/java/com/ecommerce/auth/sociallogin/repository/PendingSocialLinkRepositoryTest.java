package com.ecommerce.auth.sociallogin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.auth.sociallogin.domain.PendingSocialLink;
import com.ecommerce.auth.sociallogin.domain.SocialProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PendingSocialLinkRepositoryTest {

    @Autowired private PendingSocialLinkRepository repository;

    @Test
    void findByTokenHash_returnsSavedLink() {
        PendingSocialLink saved =
                repository.save(
                        new PendingSocialLink(
                                UUID.randomUUID(),
                                SocialProvider.NAVER,
                                "n-1",
                                "e@example.com",
                                "hash-1",
                                Instant.now().plusSeconds(600)));

        assertThat(repository.findByTokenHash("hash-1")).contains(saved);
    }
}
