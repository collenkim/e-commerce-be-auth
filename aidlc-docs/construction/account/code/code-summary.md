# Code Generation Summary — Unit: Account

## Step 1: Token Unit 리팩터링 (선행 작업)

- 신설: `src/main/java/com/ecommerce/auth/shared/OpaqueTokenGenerator.java` — 무작위 토큰 생성 + SHA-256 해시
- 변경: `TokenIssuanceService`가 기존 private `generateOpaqueToken()`/`hash()`를 제거하고 `OpaqueTokenGenerator`를 주입받아 사용
- 회귀 확인: `TokenIssuanceServiceTest` 재실행, 모두 통과

## 생성된 파일

**패키지**: `com.ecommerce.auth.account`

### 비즈니스 로직
- `domain/{Account,AccountStatus,EmailVerificationToken,PasswordResetToken}.java`
- `PasswordPolicy.java`, `PasswordEncoderConfig.java`(BCrypt cost 12), `AccountProperties.java`
- `AccountService.java` (5개 절차)
- `event/{EmailVerificationRequested,PasswordResetRequested}.java`
- `messaging/{EmailEventPublisher,EmailEventsExchangeConfig}.java`
- `exception/{DuplicateEmailException,InvalidCredentialsException,EmailNotVerifiedException,InvalidVerificationTokenException,InvalidPasswordResetTokenException,WeakPasswordException}.java`

### API 레이어
- `api/AccountController.java` — 6개 엔드포인트
- `api/AccountExceptionHandler.java`
- `api/dto/*.java`

### 리포지토리 레이어
- `repository/{AccountRepository,EmailVerificationTokenRepository,PasswordResetTokenRepository}.java`

### DB 마이그레이션
- `V2__create_account_and_tokens.sql` (account, email_verification_token, password_reset_token)

### 설정/빌드/배포
- `build.gradle.kts` — `spring-boot-starter-amqp` 추가
- `application.properties` — RabbitMQ, `app.account.*` TTL 설정 추가
- `docker-compose.yml` — `rabbitmq` 서비스 추가 (management UI 포트 15672 포함)
- `.env.example` — RabbitMQ 자격증명 추가
- `.gitignore` — `.jqwik-database`(jqwik 실패 케이스 캐시) 추가

### 테스트
- `AccountServiceTest`(13케이스), `PasswordPolicyTest`, `PasswordPolicyPropertyTest`(jqwik, PBT-03 불변식 2종)
- `AccountRepositoryTest`, `EmailVerificationTokenRepositoryTest`, `PasswordResetTokenRepositoryTest` (`@DataJpaTest`)
- `AccountControllerTest` (`@WebMvcTest`, 10케이스)

전체 70개 테스트, `./gradlew test` 통과 확인 (2회 재실행으로 안정성 확인). 진행 중 발견한 이슈들:
- **JWT 변조 테스트 결함 발견**: 기존 `JwtProviderTest.parse_tamperedToken_...`이 base64url 마지막 문자의 패딩 비트 특성상 가끔 "변조"해도 동일 바이트로 디코딩되어 간헐적으로 실패할 수 있는 결함을 발견 — 서명 구간 중간 문자를 변조하도록 수정해 결정론적으로 만듦 (Token Unit 코드 자체의 버그는 아니었음, 테스트 결함).
- **JPA `@GeneratedValue` id는 mock 기반 단위 테스트에서 채워지지 않음**을 반영해 `AccountService.resetPassword()`가 `account.getId()` 대신 이미 알고 있는 `token.getAccountId()`를 사용하도록 수정 — 실행 결과는 동일하지만 프록시/지연로딩 동작에 덜 의존적이라 더 견고함.

## API 계약

| 엔드포인트 | 설명 |
|---|---|
| `POST /api/auth/signup` | `{email, password}` → 201 `{accountId}` |
| `POST /api/auth/email/verify` | `{token}` → 204 |
| `POST /api/auth/email/verify/resend` | `{email}` → 204 (계정 존재 무관 항상 204) |
| `POST /api/auth/login` | `{email, password}` → 200 `{accessToken, refreshToken}` (Token Unit `TokenPairResponse` 재사용) |
| `POST /api/auth/password-reset/request` | `{email}` → 204 (계정 존재 무관 항상 204) |
| `POST /api/auth/password-reset/execute` | `{token, newPassword}` → 204 |

## 다음 Unit에 대한 통합 지점 / 알려진 제약

1. **Account의 6개 엔드포인트는 모두 공개(미보호)다.** Authorization Unit이 `SecurityFilterChain`에서 이 경로들을 `permitAll`로 명시해야 한다.
2. **Rate Limit 미통합.** signup/login 등은 RateLimit Unit이 빌드된 후 필터로 결합된다.
3. **SELLER/ADMIN 역할 부여 경로가 없다.** 가입은 항상 `USER`. 역할 승격은 이 프로젝트의 범위에 명시적으로 정의되지 않아 향후 관리자 기능(US-502)에서 다룰 사항으로 남겨둔다.
4. **RabbitMQ에는 큐/바인딩이 없다** — exchange 발행까지만 구현(consumer는 범위 밖). 향후 Notification 서비스가 큐를 만들어 바인딩해야 한다.
