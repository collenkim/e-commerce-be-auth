package com.ecommerce.auth.ratelimit.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * {@link AccountBlockedException}은 {@code AccountController.login()}이 {@code RateLimitService}를
 * 직접 호출하면서 던져질 수 있으므로 여기서 전역으로 처리한다. {@link IpBlockedException}/
 * {@link RateLimitExceededException}은 {@code RateLimitFilter}가 컨트롤러 도달 전에 직접 응답을
 * 작성하므로 이 핸들러까지 도달하지 않는다.
 */
@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountBlocked() {
        return ResponseEntity.status(429)
                .body(
                        new ErrorResponse(
                                "ACCOUNT_TEMPORARILY_LOCKED",
                                "This account is temporarily locked due to repeated failed logins."));
    }

    public record ErrorResponse(String code, String message) {}
}
