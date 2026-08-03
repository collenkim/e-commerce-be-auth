package com.ecommerce.auth.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.auth.account.exception.WeakPasswordException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void isValid_meetsPolicy_returnsTrue() {
        assertThat(policy.isValid("Password1")).isTrue();
    }

    @Test
    void isValid_tooShort_returnsFalse() {
        assertThat(policy.isValid("Pass1")).isFalse();
    }

    @Test
    void isValid_noDigit_returnsFalse() {
        assertThat(policy.isValid("PasswordOnly")).isFalse();
    }

    @Test
    void isValid_noLetter_returnsFalse() {
        assertThat(policy.isValid("12345678")).isFalse();
    }

    @Test
    void isValid_null_returnsFalse() {
        assertThat(policy.isValid(null)).isFalse();
    }

    @Test
    void validate_weakPassword_throws() {
        assertThatThrownBy(() -> policy.validate("weak")).isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void validate_strongPassword_doesNotThrow() {
        policy.validate("Password1");
    }
}
