package com.ecommerce.auth.sociallogin.exception;

public class InvalidLinkTokenException extends RuntimeException {
    public InvalidLinkTokenException(String message) {
        super(message);
    }
}
