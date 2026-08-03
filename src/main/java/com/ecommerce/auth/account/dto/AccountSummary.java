package com.ecommerce.auth.account.dto;

import com.ecommerce.auth.shared.Role;
import java.util.UUID;

/** 다른 Unit(SocialLogin 등)에 노출하는 계정 요약 — 전체 JPA 엔티티를 Unit 경계 밖으로 노출하지 않기 위함. */
public record AccountSummary(UUID id, String email, Role role, boolean active) {}
