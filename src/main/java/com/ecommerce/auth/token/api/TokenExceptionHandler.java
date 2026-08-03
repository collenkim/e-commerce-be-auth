package com.ecommerce.auth.token.api;

import com.ecommerce.auth.token.exception.InvalidTokenException;
import com.ecommerce.auth.token.exception.TokenReuseDetectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 프로덕션 오류 응답은 일반 메시지만 반환한다 — 내부 정보 노출 금지 (SECURITY-09). */
@RestControllerAdvice
public class TokenExceptionHandler {

    @ExceptionHandler({InvalidTokenException.class, TokenReuseDetectedException.class})
    public ResponseEntity<ErrorResponse> handleInvalidToken() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_TOKEN", "The provided token is invalid or expired."));
    }

    public record ErrorResponse(String code, String message) {}
}
