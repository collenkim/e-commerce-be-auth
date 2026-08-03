package com.ecommerce.auth.sociallogin;

import com.ecommerce.auth.account.AccountService;
import com.ecommerce.auth.account.dto.AccountSummary;
import com.ecommerce.auth.shared.OpaqueTokenGenerator;
import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.sociallogin.domain.PendingSocialLink;
import com.ecommerce.auth.sociallogin.domain.SocialAccount;
import com.ecommerce.auth.sociallogin.dto.NormalizedSocialUser;
import com.ecommerce.auth.sociallogin.dto.SocialLoginOutcome;
import com.ecommerce.auth.sociallogin.exception.InvalidLinkTokenException;
import com.ecommerce.auth.sociallogin.exception.LinkPasswordMismatchException;
import com.ecommerce.auth.sociallogin.repository.PendingSocialLinkRepository;
import com.ecommerce.auth.sociallogin.repository.SocialAccountRepository;
import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.dto.TokenPair;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SocialLogin Unit의 오케스트레이션 서비스. functional-design/business-logic-model.md의
 * 절차 2(로그인/가입/연동판단)와 절차 3(연동 확인)을 구현한다. US-201, US-202, US-203.
 */
@Service
public class SocialLoginService {

    private final SocialAccountRepository socialAccountRepository;
    private final PendingSocialLinkRepository pendingSocialLinkRepository;
    private final AccountService accountService;
    private final TokenIssuanceService tokenIssuanceService;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final SocialLoginProperties properties;
    private final Clock clock;

    public SocialLoginService(
            SocialAccountRepository socialAccountRepository,
            PendingSocialLinkRepository pendingSocialLinkRepository,
            AccountService accountService,
            TokenIssuanceService tokenIssuanceService,
            OpaqueTokenGenerator opaqueTokenGenerator,
            PasswordEncoder passwordEncoder,
            SocialLoginProperties properties,
            Clock clock) {
        this.socialAccountRepository = socialAccountRepository;
        this.pendingSocialLinkRepository = pendingSocialLinkRepository;
        this.accountService = accountService;
        this.tokenIssuanceService = tokenIssuanceService;
        this.opaqueTokenGenerator = opaqueTokenGenerator;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    /** 절차 2: 기존 연결이면 로그인, 없으면 신규 가입(즉시 연동) 또는 연동 확인 대기. */
    @Transactional
    public SocialLoginOutcome handleLogin(NormalizedSocialUser user) {
        var existingLink =
                socialAccountRepository.findByProviderAndProviderUserId(user.provider(), user.providerUserId());
        if (existingLink.isPresent()) {
            UUID accountId = existingLink.get().getAccountId();
            AccountSummary account =
                    accountService
                            .findById(accountId)
                            .orElseThrow(() -> new IllegalStateException("Linked account not found: " + accountId));
            return SocialLoginOutcome.loggedIn(tokenIssuanceService.issue(accountId, account.role()));
        }

        var matchingAccount = accountService.findByEmail(user.email());
        if (matchingAccount.isEmpty()) {
            return SocialLoginOutcome.loggedIn(createAndLinkNewAccount(user));
        }

        return SocialLoginOutcome.linkConfirmationRequired(
                createPendingLink(matchingAccount.get().id(), user));
    }

    /** 절차 3: 연동 확인 — 기존 계정 비밀번호로 소유권을 증명하면 연동 완료 후 토큰을 발급한다. */
    @Transactional
    public TokenPair confirmLink(String linkTokenPlain, String existingPassword) {
        Instant now = clock.instant();
        PendingSocialLink pendingLink =
                pendingSocialLinkRepository
                        .findByTokenHash(opaqueTokenGenerator.hash(linkTokenPlain))
                        .filter(l -> l.isUsable(now))
                        .orElseThrow(() -> new InvalidLinkTokenException("Link token invalid or expired"));

        if (!accountService.verifyPassword(pendingLink.getExistingAccountId(), existingPassword)) {
            throw new LinkPasswordMismatchException("Password does not match the existing account");
        }

        pendingLink.markConsumed(now);
        pendingSocialLinkRepository.save(pendingLink);

        socialAccountRepository.save(
                new SocialAccount(
                        pendingLink.getProvider(),
                        pendingLink.getProviderUserId(),
                        pendingLink.getExistingAccountId(),
                        now));

        AccountSummary account =
                accountService
                        .findById(pendingLink.getExistingAccountId())
                        .orElseThrow(() -> new IllegalStateException("Account not found"));
        return tokenIssuanceService.issue(account.id(), account.role());
    }

    private TokenPair createAndLinkNewAccount(NormalizedSocialUser user) {
        String randomPasswordHash = passwordEncoder.encode(opaqueTokenGenerator.generate());
        UUID accountId = accountService.createSociallyVerifiedAccount(user.email(), randomPasswordHash);

        socialAccountRepository.save(
                new SocialAccount(user.provider(), user.providerUserId(), accountId, clock.instant()));

        return tokenIssuanceService.issue(accountId, Role.USER);
    }

    private String createPendingLink(UUID existingAccountId, NormalizedSocialUser user) {
        Instant now = clock.instant();
        String tokenPlain = opaqueTokenGenerator.generate();
        pendingSocialLinkRepository.save(
                new PendingSocialLink(
                        existingAccountId,
                        user.provider(),
                        user.providerUserId(),
                        user.email(),
                        opaqueTokenGenerator.hash(tokenPlain),
                        now.plus(properties.pendingLinkTtl())));
        return tokenPlain;
    }
}
