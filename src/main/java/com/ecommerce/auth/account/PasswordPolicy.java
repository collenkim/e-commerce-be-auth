package com.ecommerce.auth.account;

import com.ecommerce.auth.account.exception.WeakPasswordException;
import org.springframework.stereotype.Component;

/** 비밀번호 정책: 최소 8자 + 문자/숫자 조합 (business-rules.md, Functional Design Q1:A). 순수 함수. */
@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 8;

    public boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }

    public void validate(String password) {
        if (!isValid(password)) {
            throw new WeakPasswordException(
                    "Password must be at least " + MIN_LENGTH + " characters and contain a letter and a digit");
        }
    }
}
