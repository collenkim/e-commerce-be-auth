package com.ecommerce.auth.sociallogin.domain;

public enum SocialProvider {
    KAKAO,
    NAVER,
    GOOGLE;

    /** Spring Security의 registrationId(소문자)를 이 enum으로 매핑한다. */
    public static SocialProvider fromRegistrationId(String registrationId) {
        return SocialProvider.valueOf(registrationId.toUpperCase(java.util.Locale.ROOT));
    }
}
