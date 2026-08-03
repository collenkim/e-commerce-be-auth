package com.ecommerce.auth.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.auth.account.domain.PasswordResetToken;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PasswordResetTokenRepositoryTest {

    @Autowired private PasswordResetTokenRepository repository;

    @Test
    void findByTokenHash_returnsSavedToken() {
        UUID accountId = UUID.randomUUID();
        PasswordResetToken token =
                repository.save(new PasswordResetToken(accountId, "hash-1", Instant.now().plusSeconds(1800)));

        assertThat(repository.findByTokenHash("hash-1")).contains(token);
    }

    @Test
    void findAllByAccountIdAndConsumedAtIsNull_excludesConsumedTokens() {
        UUID accountId = UUID.randomUUID();
        PasswordResetToken active =
                repository.save(new PasswordResetToken(accountId, "hash-active", Instant.now().plusSeconds(1800)));
        PasswordResetToken consumed =
                repository.save(new PasswordResetToken(accountId, "hash-consumed", Instant.now().plusSeconds(1800)));
        consumed.markConsumed(Instant.now());
        repository.save(consumed);

        assertThat(repository.findAllByAccountIdAndConsumedAtIsNull(accountId)).containsExactly(active);
    }
}
