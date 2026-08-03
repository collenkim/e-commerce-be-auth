package com.ecommerce.auth.sociallogin.dto;

import com.ecommerce.auth.sociallogin.domain.SocialProvider;

/** provider별 원시 사용자 정보를 정규화한 결과 — providerUserId/email만 수집한다(수집 정보 최소화 원칙). */
public record NormalizedSocialUser(SocialProvider provider, String providerUserId, String email) {}
