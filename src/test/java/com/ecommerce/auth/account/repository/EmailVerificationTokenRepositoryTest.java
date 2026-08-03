package com.ecommerce.auth.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.auth.account.domain.EmailVerificationToken;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class EmailVerificationTokenRepositoryTest {

    @Autowired private EmailVerificationTokenRepository repository;

    @Test
    void findByTokenHash_returnsSavedToken() {
        UUID accountId = UUID.randomUUID();
        EmailVerificationToken token =
                repository.save(new EmailVerificationToken(accountId, "hash-1", Instant.now().plusSeconds(3600)));

        assertThat(repository.findByTokenHash("hash-1")).contains(token);
    }

    @Test
    void findAllByAccountIdAndConsumedAtIsNull_excludesConsumedTokens() {
        UUID accountId = UUID.randomUUID();
        EmailVerificationToken active =
                repository.save(
                        new EmailVerificationToken(accountId, "hash-active", Instant.now().plusSeconds(3600)));
        EmailVerificationToken consumed =
                repository.save(
                        new EmailVerificationToken(accountId, "hash-consumed", Instant.now().plusSeconds(3600)));
        consumed.markConsumed(Instant.now());
        repository.save(consumed);

        assertThat(repository.findAllByAccountIdAndConsumedAtIsNull(accountId)).containsExactly(active);
    }
}
