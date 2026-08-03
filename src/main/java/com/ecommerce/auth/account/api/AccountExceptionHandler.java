package com.ecommerce.auth.account.api;

import com.ecommerce.auth.account.exception.AccountNotFoundException;
import com.ecommerce.auth.account.exception.DuplicateEmailException;
import com.ecommerce.auth.account.exception.EmailNotVerifiedException;
import com.ecommerce.auth.account.exception.InvalidCredentialsException;
import com.ecommerce.auth.account.exception.InvalidPasswordResetTokenException;
import com.ecommerce.auth.account.exception.InvalidVerificationTokenException;
import com.ecommerce.auth.account.exception.WeakPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 프로덕션 오류 응답은 일반 메시지만 반환한다 — 내부 정보 노출 금지 (SECURITY-09). */
@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("EMAIL_ALREADY_REGISTERED", "This email is already registered."));
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ErrorResponse> handleWeakPassword() {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("WEAK_PASSWORD", "Password does not meet the required policy."));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password."));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("EMAIL_NOT_VERIFIED", "Email verification is required before login."));
    }

    @ExceptionHandler({InvalidVerificationTokenException.class, InvalidPasswordResetTokenException.class})
    public ResponseEntity<ErrorResponse> handleInvalidToken() {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_TOKEN", "The provided token is invalid or expired."));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ACCOUNT_NOT_FOUND", "The specified account does not exist."));
    }

    public record ErrorResponse(String code, String message) {}
}
