package com.ecommerce.auth.account.exception;

/** 계정 없음/비밀번호 불일치를 구분 없이 나타낸다 (계정 열거 공격 방지, SECURITY-12, US-103 AC3). */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
