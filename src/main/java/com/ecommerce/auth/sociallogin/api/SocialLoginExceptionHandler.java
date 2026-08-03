package com.ecommerce.auth.sociallogin.api;

import com.ecommerce.auth.sociallogin.exception.InvalidLinkTokenException;
import com.ecommerce.auth.sociallogin.exception.LinkPasswordMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 프로덕션 오류 응답은 일반 메시지만 반환한다 (SECURITY-09). */
@RestControllerAdvice
public class SocialLoginExceptionHandler {

    @ExceptionHandler(InvalidLinkTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLinkToken() {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_LINK_TOKEN", "The link confirmation token is invalid or expired."));
    }

    @ExceptionHandler(LinkPasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handleLinkPasswordMismatch() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("LINK_PASSWORD_MISMATCH", "The existing account password does not match."));
    }

    public record ErrorResponse(String code, String message) {}
}
