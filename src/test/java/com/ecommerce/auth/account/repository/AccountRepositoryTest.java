package com.ecommerce.auth.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.auth.account.domain.Account;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired private AccountRepository repository;

    @Test
    void findByEmail_returnsSavedAccount() {
        Account account = repository.save(Account.signUp("user@example.com", "hash", Instant.now()));

        assertThat(repository.findByEmail("user@example.com")).contains(account);
    }

    @Test
    void existsByEmail_reflectsSavedState() {
        repository.save(Account.signUp("exists@example.com", "hash", Instant.now()));

        assertThat(repository.existsByEmail("exists@example.com")).isTrue();
        assertThat(repository.existsByEmail("missing@example.com")).isFalse();
    }
}
