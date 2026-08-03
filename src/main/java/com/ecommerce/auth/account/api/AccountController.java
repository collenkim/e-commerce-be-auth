package com.ecommerce.auth.account.api;

import com.ecommerce.auth.account.AccountService;
import com.ecommerce.auth.account.api.dto.EmailOnlyRequest;
import com.ecommerce.auth.account.api.dto.LoginRequest;
import com.ecommerce.auth.account.api.dto.PasswordResetExecuteRequest;
import com.ecommerce.auth.account.api.dto.SignUpRequest;
import com.ecommerce.auth.account.api.dto.SignUpResponse;
import com.ecommerce.auth.account.api.dto.VerifyEmailRequest;
import com.ecommerce.auth.account.exception.InvalidCredentialsException;
import com.ecommerce.auth.ratelimit.RateLimitService;
import com.ecommerce.auth.token.api.dto.TokenPairResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-101(가입), US-102(이메일 인증), US-103(로그인), US-104(비밀번호 재설정) 공개 API.
 *
 * <p>{@code login()}은 RateLimit Unit의 {@link RateLimitService}를 직접 호출해 계정 기준 차단(US-402)과
 * 브루트포스 IP 차단(US-404)을 처리한다 — IP 기준 검사(US-401/403)는 {@code RateLimitFilter}가
 * 이 컨트롤러 앞단에서 이미 처리한다(ratelimit/functional-design/business-logic-model.md 참고).
 */
@RestController
public class AccountController {

    private final AccountService accountService;
    private final RateLimitService rateLimitService;

    public AccountController(AccountService accountService, RateLimitService rateLimitService) {
        this.accountService = accountService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return new SignUpResponse(accountService.signUp(request.email(), request.password()));
    }

    @PostMapping("/api/auth/email/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        accountService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/email/verify/resend")
    public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody EmailOnlyRequest request) {
        accountService.resendVerificationEmail(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        rateLimitService.assertAccountNotBlocked(request.email());
        try {
            return TokenPairResponse.from(accountService.login(request.email(), request.password()));
        } catch (InvalidCredentialsException ex) {
            rateLimitService.recordLoginFailure(servletRequest.getRemoteAddr(), request.email());
            throw ex;
        }
    }

    @PostMapping("/api/auth/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody EmailOnlyRequest request) {
        accountService.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/password-reset/execute")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetExecuteRequest request) {
        accountService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
