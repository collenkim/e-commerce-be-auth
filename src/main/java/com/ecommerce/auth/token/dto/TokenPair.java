package com.ecommerce.auth.token.dto;

/** 발급/회전 결과로 클라이언트에 반환되는 토큰 쌍. */
public record TokenPair(String accessToken, String refreshToken) {}
