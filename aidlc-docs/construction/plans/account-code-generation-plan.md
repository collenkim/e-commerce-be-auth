# Code Generation Plan — Unit: Account

**패키지**: `com.ecommerce.auth.account` (+ 공유 `com.ecommerce.auth.shared`)
**구현 스토리**: US-101, US-102, US-103, US-104
**의존 Unit**: Token (완료됨) — `TokenIssuanceService.issue()`, `.revokeAllForAccount()` 호출

**이 Unit이 노출하는 API**:
- `POST /api/auth/signup`, `POST /api/auth/email/verify`, `POST /api/auth/email/verify/resend`
- `POST /api/auth/login`
- `POST /api/auth/password-reset/request`, `POST /api/auth/password-reset/execute`

**범위 경계**: Rate Limit 미통합(RateLimit Unit 책임), 인증 필터 미배선(Authorization Unit 책임) — Token Unit과 동일 패턴.

## Steps

- [x] Step 1: 공유 유틸리티 추출 — `com.ecommerce.auth.shared.OpaqueTokenGenerator` 신설, **Token Unit `TokenIssuanceService` 리팩터링**(기존 private 메서드 제거 후 재사용), 기존 `TokenIssuanceServiceTest` 재실행으로 회귀 없음 확인
- [x] Step 2: Business Logic Generation — `Account`/`AccountStatus`, `EmailVerificationToken`, `PasswordResetToken`, `PasswordPolicy`, `AccountService`(5개 절차), `EmailEventPublisher`, 이벤트 레코드, 예외 클래스
- [x] Step 3: Business Logic Unit Testing — `AccountServiceTest`(Mockito), `PasswordPolicyTest`(+jqwik PBT-03 불변식: 정책 통과 비밀번호는 항상 8자 이상)
- [x] Step 4: Business Logic Summary
- [x] Step 5: API Layer Generation — `AccountController`, DTO, `AccountExceptionHandler`
- [x] Step 6: API Layer Unit Testing — `@WebMvcTest`
- [x] Step 7: API Layer Summary
- [x] Step 8: Repository Layer Generation — `AccountRepository`, `EmailVerificationTokenRepository`, `PasswordResetTokenRepository`
- [x] Step 9: Repository Layer Unit Testing — `@DataJpaTest`
- [x] Step 10: Repository Layer Summary
- [x] Step 11: Database Migration Script — Flyway `V2__create_account_and_tokens.sql`
- [x] Step 12: Documentation Generation — `aidlc-docs/construction/account/code/code-summary.md`
- [x] Step 13: Deployment Artifacts — `docker-compose.yml`에 `rabbitmq` 추가, `application.properties`에 RabbitMQ/토큰 TTL 설정 추가, `.env.example` 갱신
