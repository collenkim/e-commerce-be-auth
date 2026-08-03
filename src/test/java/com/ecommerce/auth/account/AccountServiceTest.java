package com.ecommerce.auth.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.auth.account.domain.Account;
import com.ecommerce.auth.account.domain.EmailVerificationToken;
import com.ecommerce.auth.account.domain.PasswordResetToken;
import com.ecommerce.auth.account.event.EmailVerificationRequested;
import com.ecommerce.auth.account.event.PasswordResetRequested;
import com.ecommerce.auth.account.exception.DuplicateEmailException;
import com.ecommerce.auth.account.exception.EmailNotVerifiedException;
import com.ecommerce.auth.account.exception.InvalidCredentialsException;
import com.ecommerce.auth.account.exception.InvalidPasswordResetTokenException;
import com.ecommerce.auth.account.exception.InvalidVerificationTokenException;
import com.ecommerce.auth.account.exception.WeakPasswordException;
import com.ecommerce.auth.account.messaging.EmailEventPublisher;
import com.ecommerce.auth.account.repository.AccountRepository;
import com.ecommerce.auth.account.repository.EmailVerificationTokenRepository;
import com.ecommerce.auth.account.repository.PasswordResetTokenRepository;
import com.ecommerce.auth.shared.OpaqueTokenGenerator;
import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.dto.TokenPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private AccountRepository accountRepository;
    private EmailVerificationTokenRepository verificationTokenRepository;
    private PasswordResetTokenRepository resetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private EmailEventPublisher emailEventPublisher;
    private TokenIssuanceService tokenIssuanceService;
    private AccountService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        verificationTokenRepository = mock(EmailVerificationTokenRepository.class);
        resetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailEventPublisher = mock(EmailEventPublisher.class);
        tokenIssuanceService = mock(TokenIssuanceService.class);
        AccountProperties properties = new AccountProperties(Duration.ofHours(24), Duration.ofMinutes(30));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationTokenRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(resetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service =
                new AccountService(
                        accountRepository,
                        verificationTokenRepository,
                        resetTokenRepository,
                        passwordEncoder,
                        new PasswordPolicy(),
                        new OpaqueTokenGenerator(),
                        emailEventPublisher,
                        tokenIssuanceService,
                        properties,
                        clock);
    }

    @Test
    void signUp_newEmail_createsAccountAndPublishesVerificationEvent() {
        when(accountRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(verificationTokenRepository.findAllByAccountIdAndConsumedAtIsNull(any()))
                .thenReturn(List.of());

        service.signUp("User@Example.com", "Password1");

        // 실제 id는 JPA @GeneratedValue가 영속화 시점에 채운다 - 이 mock 기반 단위 테스트는 그 값 대신
        // 저장/이벤트 발행 호출 자체가 올바르게 일어나는지를 검증한다.
        verify(accountRepository).save(any(Account.class));
        verify(emailEventPublisher).publish(any(EmailVerificationRequested.class));
    }

    @Test
    void signUp_duplicateEmail_throws() {
        when(accountRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.signUp("user@example.com", "Password1"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void signUp_weakPassword_throwsWithoutTouchingRepository() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.signUp("user@example.com", "weak"))
                .isInstanceOf(WeakPasswordException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void verifyEmail_usableToken_activatesAccount() {
        UUID accountId = UUID.randomUUID();
        EmailVerificationToken token =
                new EmailVerificationToken(accountId, "hash", NOW.plusSeconds(3600));
        when(verificationTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        Account account = Account.signUp("user@example.com", "hashed", NOW.minusSeconds(60));
        when(accountRepository.getReferenceById(accountId)).thenReturn(account);

        service.verifyEmail("plain-token");

        assertThat(token.getConsumedAt()).isEqualTo(NOW);
        assertThat(account.isActive()).isTrue();
    }

    @Test
    void verifyEmail_expiredToken_throws() {
        EmailVerificationToken expired =
                new EmailVerificationToken(UUID.randomUUID(), "hash", NOW.minusSeconds(1));
        when(verificationTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.verifyEmail("plain-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void login_correctPasswordAndActiveAccount_issuesTokens() {
        Account account = Account.signUp("user@example.com", "hashed", NOW.minusSeconds(120));
        account.activate(NOW.minusSeconds(60));
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password1", "hashed")).thenReturn(true);
        when(tokenIssuanceService.issue(account.getId(), Role.USER))
                .thenReturn(new TokenPair("access", "refresh"));

        TokenPair result = service.login("User@Example.com", "Password1");

        assertThat(result.accessToken()).isEqualTo("access");
        verify(tokenIssuanceService).issue(account.getId(), Role.USER);
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("nobody@example.com", "Password1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        Account account = Account.signUp("user@example.com", "hashed", NOW);
        account.activate(NOW);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login("user@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unverifiedAccount_throwsEmailNotVerified() {
        Account account = Account.signUp("user@example.com", "hashed", NOW); // PENDING_VERIFICATION
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password1", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> service.login("user@example.com", "Password1"))
                .isInstanceOf(EmailNotVerifiedException.class);
        verify(tokenIssuanceService, never()).issue(any(), any());
    }

    @Test
    void requestPasswordReset_existingAccount_savesTokenAndPublishesEvent() {
        Account account = Account.signUp("user@example.com", "hashed", NOW);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(resetTokenRepository.findAllByAccountIdAndConsumedAtIsNull(account.getId()))
                .thenReturn(List.of());

        service.requestPasswordReset("user@example.com");

        verify(resetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailEventPublisher).publish(any(PasswordResetRequested.class));
    }

    @Test
    void requestPasswordReset_unknownEmail_doesNothingSilently() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        service.requestPasswordReset("nobody@example.com");

        verify(resetTokenRepository, never()).save(any());
        verify(emailEventPublisher, never()).publish(any(PasswordResetRequested.class));
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndRevokesAllRefreshTokens() {
        UUID accountId = UUID.randomUUID();
        PasswordResetToken token = new PasswordResetToken(accountId, "hash", NOW.plusSeconds(600));
        when(resetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        Account account = Account.signUp("user@example.com", "old-hash", NOW.minusSeconds(600));
        when(accountRepository.getReferenceById(accountId)).thenReturn(account);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-hash");

        service.resetPassword("plain-token", "NewPassword1");

        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.getConsumedAt()).isEqualTo(NOW);
        verify(tokenIssuanceService).revokeAllForAccount(accountId);
    }

    @Test
    void resetPassword_expiredToken_throwsAndDoesNotTouchTokenIssuance() {
        PasswordResetToken expired =
                new PasswordResetToken(UUID.randomUUID(), "hash", NOW.minusSeconds(1));
        when(resetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword("plain-token", "NewPassword1"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        verify(tokenIssuanceService, never()).revokeAllForAccount(any());
    }

    @Test
    void changeRole_existingAccount_updatesRole() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.signUp("user@example.com", "hash", NOW);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        service.changeRole(accountId, Role.ADMIN);

        assertThat(account.getRole()).isEqualTo(Role.ADMIN);
        verify(accountRepository).save(account);
    }

    @Test
    void changeRole_unknownAccount_throws() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(accountId, Role.ADMIN))
                .isInstanceOf(com.ecommerce.auth.account.exception.AccountNotFoundException.class);
    }
}
