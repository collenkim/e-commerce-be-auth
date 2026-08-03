package com.ecommerce.auth.token.exception;

/**
 * 이미 회전(ROTATED)되었거나 무효화(REVOKED)된 Refresh Token이 다시 사용됐을 때 발생.
 * 발생 시 해당 토큰 패밀리 전체가 무효화된다 (business-logic-model.md 3번 절차).
 */
public class TokenReuseDetectedException extends RuntimeException {

    public TokenReuseDetectedException(String message) {
        super(message);
    }
}
