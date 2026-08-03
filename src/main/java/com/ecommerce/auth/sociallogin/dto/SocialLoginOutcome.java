package com.ecommerce.auth.sociallogin.dto;

import com.ecommerce.auth.token.dto.TokenPair;

/**
 * 소셜 로그인 처리 결과. {@code linkRequired=true}이면 즉시 로그인하지 않고 계정 연동 확인이
 * 필요하다는 뜻이다(business-logic-model.md 2번 절차).
 */
public record SocialLoginOutcome(boolean linkRequired, TokenPair tokens, String linkTokenPlain) {

    public static SocialLoginOutcome loggedIn(TokenPair tokens) {
        return new SocialLoginOutcome(false, tokens, null);
    }

    public static SocialLoginOutcome linkConfirmationRequired(String linkTokenPlain) {
        return new SocialLoginOutcome(true, null, linkTokenPlain);
    }
}
