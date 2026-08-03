package com.ecommerce.auth.sociallogin.exception;

/** 연동 확인 시 기존 계정 비밀번호가 일치하지 않을 때. 이 시점엔 계정 존재가 이미 전제되므로 구체적으로 안내한다. */
public class LinkPasswordMismatchException extends RuntimeException {
    public LinkPasswordMismatchException(String message) {
        super(message);
    }
}
