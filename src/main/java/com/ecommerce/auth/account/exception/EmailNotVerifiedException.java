package com.ecommerce.auth.account.exception;

/** 이메일 미인증 계정으로 로그인을 시도할 때 (US-102 AC3) — 자격증명 오류와 달리 구체적으로 안내한다. */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
