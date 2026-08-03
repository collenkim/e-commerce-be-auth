package com.ecommerce.auth.account;

import com.ecommerce.auth.account.domain.Account;
import com.ecommerce.auth.account.domain.EmailVerificationToken;
import com.ecommerce.auth.account.domain.PasswordResetToken;
import com.ecommerce.auth.account.dto.AccountSummary;
import com.ecommerce.auth.account.event.EmailVerificationRequested;
import com.ecommerce.auth.account.event.PasswordResetRequested;
import com.ecommerce.auth.account.exception.AccountNotFoundException;
import com.ecommerce.auth.account.exception.DuplicateEmailException;
import com.ecommerce.auth.account.exception.EmailNotVerifiedException;
import com.ecommerce.auth.account.exception.InvalidCredentialsException;
import com.ecommerce.auth.account.exception.InvalidPasswordResetTokenException;
import com.ecommerce.auth.account.exception.InvalidVerificationTokenException;
import com.ecommerce.auth.account.messaging.EmailEventPublisher;
import com.ecommerce.auth.account.repository.AccountRepository;
import com.ecommerce.auth.account.repository.EmailVerificationTokenRepository;
import com.ecommerce.auth.account.repository.PasswordResetTokenRepository;
import com.ecommerce.auth.shared.OpaqueTokenGenerator;
import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.dto.TokenPair;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account Unit의 오케스트레이션 서비스. functional-design/business-logic-model.md의 5개 절차를 구현한다.
 * US-101, US-102, US-103, US-104.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final EmailEventPublisher emailEventPublisher;
    private final TokenIssuanceService tokenIssuanceService;
    private final AccountProperties accountProperties;
    private final Clock clock;

    public AccountService(
            AccountRepository accountRepository,
            EmailVerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            OpaqueTokenGenerator opaqueTokenGenerator,
            EmailEventPublisher emailEventPublisher,
            TokenIssuanceService tokenIssuanceService,
            AccountProperties accountProperties,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.opaqueTokenGenerator = opaqueTokenGenerator;
        this.emailEventPublisher = emailEventPublisher;
        this.tokenIssuanceService = tokenIssuanceService;
        this.accountProperties = accountProperties;
        this.clock = clock;
    }

    /** 절차 1: 회원가입 (US-101). */
    @Transactional
    public UUID signUp(String email, String rawPassword) {
        String normalizedEmail = normalize(email);
        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email already registered");
        }
        passwordPolicy.validate(rawPassword);

        Instant now = clock.instant();
        Account account =
                accountRepository.save(
                        Account.signUp(normalizedEmail, passwordEncoder.encode(rawPassword), now));

        issueVerificationTokenAndPublish(account, now);
        return account.getId();
    }

    /**
     * 이메일 인증 재발송. 아직 로그인할 수 없는 미인증 사용자가 이메일로 스스로를 식별한다.
     * 계정 존재 여부와 무관하게 항상 정상 반환한다(재설정 요청과 동일한 비노출 원칙).
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        accountRepository
                .findByEmail(normalize(email))
                .filter(account -> !account.isActive())
                .ifPresent(account -> issueVerificationTokenAndPublish(account, clock.instant()));
    }

    /** 절차 2: 이메일 인증 (US-102). */
    @Transactional
    public void verifyEmail(String verificationTokenPlain) {
        Instant now = clock.instant();
        EmailVerificationToken token =
                verificationTokenRepository
                        .findByTokenHash(opaqueTokenGenerator.hash(verificationTokenPlain))
                        .filter(t -> t.isUsable(now))
                        .orElseThrow(() -> new InvalidVerificationTokenException(
                                "Verification token invalid or expired"));

        token.markConsumed(now);
        verificationTokenRepository.save(token);

        Account account = accountRepository.getReferenceById(token.getAccountId());
        account.activate(now);
        accountRepository.save(account);
    }

    /** 절차 3: 로그인 자격증명 검증 (US-103) — 성공 시 Token Unit에 토큰 발급을 위임한다. */
    public TokenPair login(String email, String rawPassword) {
        Account account =
                accountRepository
                        .findByEmail(normalize(email))
                        .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!account.isActive()) {
            throw new EmailNotVerifiedException("Email verification required before login");
        }

        return tokenIssuanceService.issue(account.getId(), account.getRole());
    }

    /** 절차 4: 비밀번호 재설정 요청 (US-104) — 계정 존재 여부와 무관하게 항상 정상 반환한다. */
    @Transactional
    public void requestPasswordReset(String email) {
        accountRepository
                .findByEmail(normalize(email))
                .ifPresent(
                        account -> {
                            Instant now = clock.instant();
                            resetTokenRepository
                                    .findAllByAccountIdAndConsumedAtIsNull(account.getId())
                                    .forEach(t -> t.markConsumed(now));

                            String tokenPlain = opaqueTokenGenerator.generate();
                            resetTokenRepository.save(
                                    new PasswordResetToken(
                                            account.getId(),
                                            opaqueTokenGenerator.hash(tokenPlain),
                                            now.plus(accountProperties.passwordResetTokenTtl())));

                            emailEventPublisher.publish(
                                    new PasswordResetRequested(account.getId(), account.getEmail(), tokenPlain));
                        });
    }

    /** 절차 5: 비밀번호 재설정 실행 (US-104) — 성공 시 Token Unit에 전체 무효화를 위임한다. */
    @Transactional
    public void resetPassword(String resetTokenPlain, String newPassword) {
        Instant now = clock.instant();
        PasswordResetToken token =
                resetTokenRepository
                        .findByTokenHash(opaqueTokenGenerator.hash(resetTokenPlain))
                        .filter(t -> t.isUsable(now))
                        .orElseThrow(() -> new InvalidPasswordResetTokenException(
                                "Password reset token invalid or expired"));

        passwordPolicy.validate(newPassword);

        Account account = accountRepository.getReferenceById(token.getAccountId());
        account.changePasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        token.markConsumed(now);
        resetTokenRepository.save(token);

        tokenIssuanceService.revokeAllForAccount(token.getAccountId());
    }

    /** SocialLogin Unit용 — 동일 이메일의 기존 계정 존재 여부 확인(연동 판단). */
    public Optional<AccountSummary> findByEmail(String email) {
        return accountRepository
                .findByEmail(normalize(email))
                .map(a -> new AccountSummary(a.getId(), a.getEmail(), a.getRole(), a.isActive()));
    }

    /** SocialLogin Unit용 — 이미 연동된 계정의 현재 역할 등을 조회(토큰 재발급 시 필요). */
    public Optional<AccountSummary> findById(UUID accountId) {
        return accountRepository
                .findById(accountId)
                .map(a -> new AccountSummary(a.getId(), a.getEmail(), a.getRole(), a.isActive()));
    }

    /**
     * SocialLogin Unit용 — provider가 이미 이메일 소유를 확인했으므로 이메일 인증 절차 없이 즉시
     * {@code ACTIVE} 계정을 생성한다. {@code randomPasswordHash}는 사용자가 알 수 없는 무작위 값이어야 한다
     * (소셜 전용 계정 규칙, sociallogin/functional-design/business-rules.md 참고).
     */
    @Transactional
    public UUID createSociallyVerifiedAccount(String email, String randomPasswordHash) {
        String normalizedEmail = normalize(email);
        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email already registered");
        }
        Instant now = clock.instant();
        Account account = Account.signUp(normalizedEmail, randomPasswordHash, now);
        account.activate(now);
        return accountRepository.save(account).getId();
    }

    /** Authorization Unit용 — 관리자가 계정의 역할을 변경한다 (US-502). */
    @Transactional
    public void changeRole(UUID accountId, Role newRole) {
        Account account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        account.changeRole(newRole);
        accountRepository.save(account);
    }

    /** SocialLogin Unit용 — 계정 연동 확인 시 기존 계정 비밀번호 검증. */
    public boolean verifyPassword(UUID accountId, String rawPassword) {
        return accountRepository
                .findById(accountId)
                .map(a -> passwordEncoder.matches(rawPassword, a.getPasswordHash()))
                .orElse(false);
    }

    private void issueVerificationTokenAndPublish(Account account, Instant now) {
        List<EmailVerificationToken> existing =
                verificationTokenRepository.findAllByAccountIdAndConsumedAtIsNull(account.getId());
        existing.forEach(t -> t.markConsumed(now));
        verificationTokenRepository.saveAll(existing);

        String tokenPlain = opaqueTokenGenerator.generate();
        verificationTokenRepository.save(
                new EmailVerificationToken(
                        account.getId(),
                        opaqueTokenGenerator.hash(tokenPlain),
                        now.plus(accountProperties.emailVerificationTokenTtl())));

        emailEventPublisher.publish(
                new EmailVerificationRequested(account.getId(), account.getEmail(), tokenPlain));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
