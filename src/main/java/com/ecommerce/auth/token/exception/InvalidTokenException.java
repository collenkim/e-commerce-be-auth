package com.ecommerce.auth.token.exception;

/** Refresh Token이 존재하지 않거나, 만료됐거나, 서명이 유효하지 않을 때 발생. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
