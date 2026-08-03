package com.ecommerce.auth.account.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 이메일 인증 재발송, 비밀번호 재설정 요청 등 email만 필요한 요청에 공통 사용. */
public record EmailOnlyRequest(@NotBlank @Email @Size(max = 254) String email) {}
