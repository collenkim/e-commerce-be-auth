package com.ecommerce.auth.sociallogin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.auth.account.AccountService;
import com.ecommerce.auth.account.dto.AccountSummary;
import com.ecommerce.auth.shared.OpaqueTokenGenerator;
import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.sociallogin.domain.PendingSocialLink;
import com.ecommerce.auth.sociallogin.domain.SocialAccount;
import com.ecommerce.auth.sociallogin.domain.SocialProvider;
import com.ecommerce.auth.sociallogin.dto.NormalizedSocialUser;
import com.ecommerce.auth.sociallogin.dto.SocialLoginOutcome;
import com.ecommerce.auth.sociallogin.exception.InvalidLinkTokenException;
import com.ecommerce.auth.sociallogin.exception.LinkPasswordMismatchException;
import com.ecommerce.auth.sociallogin.repository.PendingSocialLinkRepository;
import com.ecommerce.auth.sociallogin.repository.SocialAccountRepository;
import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.dto.TokenPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SocialLoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private SocialAccountRepository socialAccountRepository;
    private PendingSocialLinkRepository pendingSocialLinkRepository;
    private AccountService accountService;
    private TokenIssuanceService tokenIssuanceService;
    private PasswordEncoder passwordEncoder;
    private SocialLoginService service;

    @BeforeEach
    void setUp() {
        socialAccountRepository = mock(SocialAccountRepository.class);
        pendingSocialLinkRepository = mock(PendingSocialLinkRepository.class);
        accountService = mock(AccountService.class);
        tokenIssuanceService = mock(TokenIssuanceService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        SocialLoginProperties properties = new SocialLoginProperties(Duration.ofMinutes(10), "http://fe/callback");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        when(socialAccountRepository.save(any(SocialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pendingSocialLinkRepository.save(any(PendingSocialLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");

        service =
                new SocialLoginService(
                        socialAccountRepository,
                        pendingSocialLinkRepository,
                        accountService,
                        tokenIssuanceService,
                        new OpaqueTokenGenerator(),
                        passwordEncoder,
                        properties,
                        clock);
    }

    @Test
    void handleLogin_existingLink_logsInDirectly() {
        UUID accountId = UUID.randomUUID();
        SocialAccount link = new SocialAccount(SocialProvider.GOOGLE, "google-123", accountId, NOW.minusSeconds(60));
        when(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(link));
        when(accountService.findById(accountId))
                .thenReturn(Optional.of(new AccountSummary(accountId, "user@example.com", Role.USER, true)));
        when(tokenIssuanceService.issue(accountId, Role.USER)).thenReturn(new TokenPair("access", "refresh"));

        SocialLoginOutcome outcome =
                service.handleLogin(new NormalizedSocialUser(SocialProvider.GOOGLE, "google-123", "user@example.com"));

        assertThat(outcome.linkRequired()).isFalse();
        assertThat(outcome.tokens().accessToken()).isEqualTo("access");
        verify(accountService, never()).createSociallyVerifiedAccount(anyString(), anyString());
    }

    @Test
    void handleLogin_noLinkAndNoMatchingEmail_createsNewAccountAndLogsIn() {
        when(socialAccountRepository.findByProviderAndProviderUserId(any(), anyString()))
                .thenReturn(Optional.empty());
        when(accountService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        UUID newAccountId = UUID.randomUUID();
        when(accountService.createSociallyVerifiedAccount(eq("new@example.com"), anyString()))
                .thenReturn(newAccountId);
        when(tokenIssuanceService.issue(newAccountId, Role.USER)).thenReturn(new TokenPair("access2", "refresh2"));

        SocialLoginOutcome outcome =
                service.handleLogin(new NormalizedSocialUser(SocialProvider.KAKAO, "kakao-1", "new@example.com"));

        assertThat(outcome.linkRequired()).isFalse();
        assertThat(outcome.tokens().accessToken()).isEqualTo("access2");
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }

    @Test
    void handleLogin_noLinkButMatchingEmail_requiresLinkConfirmation() {
        UUID existingAccountId = UUID.randomUUID();
        when(socialAccountRepository.findByProviderAndProviderUserId(any(), anyString()))
                .thenReturn(Optional.empty());
        when(accountService.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(new AccountSummary(existingAccountId, "existing@example.com", Role.USER, true)));

        SocialLoginOutcome outcome =
                service.handleLogin(
                        new NormalizedSocialUser(SocialProvider.NAVER, "naver-1", "existing@example.com"));

        assertThat(outcome.linkRequired()).isTrue();
        assertThat(outcome.linkTokenPlain()).isNotBlank();
        verify(accountService, never()).createSociallyVerifiedAccount(anyString(), anyString());
        verify(pendingSocialLinkRepository).save(any(PendingSocialLink.class));
    }

    @Test
    void confirmLink_correctPassword_linksAndIssuesTokens() {
        UUID accountId = UUID.randomUUID();
        PendingSocialLink pending =
                new PendingSocialLink(
                        accountId, SocialProvider.NAVER, "naver-1", "existing@example.com", "hash", NOW.plusSeconds(300));
        when(pendingSocialLinkRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));
        when(accountService.verifyPassword(accountId, "correct-password")).thenReturn(true);
        when(accountService.findById(accountId))
                .thenReturn(Optional.of(new AccountSummary(accountId, "existing@example.com", Role.USER, true)));
        when(tokenIssuanceService.issue(accountId, Role.USER)).thenReturn(new TokenPair("a", "r"));

        TokenPair result = service.confirmLink("plain-link-token", "correct-password");

        assertThat(result.accessToken()).isEqualTo("a");
        assertThat(pending.getConsumedAt()).isEqualTo(NOW);
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }

    @Test
    void confirmLink_expiredToken_throws() {
        PendingSocialLink expired =
                new PendingSocialLink(
                        UUID.randomUUID(), SocialProvider.NAVER, "naver-1", "e@example.com", "hash", NOW.minusSeconds(1));
        when(pendingSocialLinkRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.confirmLink("plain-link-token", "any"))
                .isInstanceOf(InvalidLinkTokenException.class);
    }

    @Test
    void confirmLink_wrongPassword_throwsAndDoesNotCreateLink() {
        UUID accountId = UUID.randomUUID();
        PendingSocialLink pending =
                new PendingSocialLink(
                        accountId, SocialProvider.NAVER, "naver-1", "e@example.com", "hash", NOW.plusSeconds(300));
        when(pendingSocialLinkRepository.findByTokenHash(anyString())).thenReturn(Optional.of(pending));
        when(accountService.verifyPassword(accountId, "wrong")).thenReturn(false);

        assertThatThrownBy(() -> service.confirmLink("plain-link-token", "wrong"))
                .isInstanceOf(LinkPasswordMismatchException.class);
        verify(socialAccountRepository, never()).save(any());
    }
}
