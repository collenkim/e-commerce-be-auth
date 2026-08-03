package com.ecommerce.auth.token.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.domain.RefreshToken;
import com.ecommerce.auth.token.domain.RefreshTokenStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired private RefreshTokenRepository repository;

    @Test
    void findByTokenHash_returnsSavedToken() {
        RefreshToken token =
                repository.save(
                        RefreshToken.issueNewFamily(
                                UUID.randomUUID(),
                                Role.USER,
                                "hash-abc",
                                Instant.now(),
                                Instant.now().plusSeconds(600)));

        assertThat(repository.findByTokenHash("hash-abc")).contains(token);
    }

    @Test
    void findAllByFamilyId_returnsAllTokensInFamily() {
        UUID accountId = UUID.randomUUID();
        RefreshToken first =
                repository.save(
                        RefreshToken.issueNewFamily(
                                accountId, Role.USER, "hash-1", Instant.now(), Instant.now().plusSeconds(600)));
        RefreshToken second =
                repository.save(
                        first.rotateToNew("hash-2", Instant.now(), Instant.now().plusSeconds(600)));

        List<RefreshToken> family = repository.findAllByFamilyId(first.getFamilyId());

        assertThat(family).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void findAllByAccountIdAndStatus_filtersByStatus() {
        UUID accountId = UUID.randomUUID();
        RefreshToken active =
                repository.save(
                        RefreshToken.issueNewFamily(
                                accountId, Role.USER, "hash-active", Instant.now(), Instant.now().plusSeconds(600)));
        RefreshToken revoked =
                repository.save(
                        RefreshToken.issueNewFamily(
                                accountId, Role.USER, "hash-revoked", Instant.now(), Instant.now().plusSeconds(600)));
        revoked.markRevoked();
        repository.save(revoked);

        List<RefreshToken> activeTokens =
                repository.findAllByAccountIdAndStatus(accountId, RefreshTokenStatus.ACTIVE);

        assertThat(activeTokens).containsExactly(active);
    }
}
