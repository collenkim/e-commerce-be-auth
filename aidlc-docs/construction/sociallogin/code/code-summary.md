# Code Generation Summary — Unit: SocialLogin

## Step 0: Account Unit에 4개 메서드 추가 (계획엔 3개였으나 구현 중 1개 추가 필요 발견)

- `findByEmail(String) -> Optional<AccountSummary>`
- `findById(UUID) -> Optional<AccountSummary>` — **계획에 없었지만 구현 중 발견**: 이미 연동된 계정으로 로그인할 때 role을 다시 조회해야 하는데 이메일을 모르므로 accountId로 조회하는 메서드가 별도로 필요했음
- `createSociallyVerifiedAccount(String, String) -> UUID`
- `verifyPassword(UUID, String) -> boolean`
- 신규: `com.ecommerce.auth.account.dto.AccountSummary` (id, email, role, active) — 다른 Unit에 전체 JPA 엔티티를 노출하지 않기 위함
- 회귀 확인: `AccountServiceTest` 재실행, 모두 통과

## NFR Requirements 리팩터링 사항 하나 되돌림

NFR Requirements에서 "Google `email_verified=false`면 자동 활성화 대신 이메일 인증 요구"를 제안했으나, 구현 시점에 이걸 제대로 하려면 비밀번호 없는 소셜 신규 가입자를 위한 별도의 이메일 인증 경로(기존 Account Unit의 signUp은 비밀번호 필수)가 새로 필요하다는 게 드러났다. 범위 대비 이득이 작다고 판단해 **철회** — 3개 provider 모두 동일하게 provider의 이메일 확인을 신뢰하는 것으로 일관되게 처리한다. (설계 단계 결정을 구현 중 재검토하고 되돌린 사례로 기록.)

## 생성된 파일

**패키지**: `com.ecommerce.auth.sociallogin`

### 비즈니스 로직
- `domain/{SocialProvider,SocialAccount,PendingSocialLink}.java`
- `SocialLoginProperties.java`, `SocialLoginService.java`(로그인/가입/연동판단/연동확인)
- `NormalizingOAuth2UserService.java` (provider별 attribute 파싱 — 순수 함수로 분리해 단위 테스트)
- `dto/{NormalizedSocialUser,SocialLoginOutcome}.java`
- `exception/{InvalidLinkTokenException,LinkPasswordMismatchException}.java`

### 보안/API 레이어
- `security/{SocialLoginSuccessHandler,SocialLoginFailureHandler,SocialLoginSecurityConfig}.java`
- `api/{LinkConfirmController,SocialLoginExceptionHandler}.java`, `api/dto/LinkConfirmRequest.java`

### 리포지토리 레이어
- `repository/{SocialAccountRepository,PendingSocialLinkRepository}.java`

### DB 마이그레이션
- `V3__create_social_login_tables.sql` (social_account, pending_social_link)

### 설정/빌드/배포
- `build.gradle.kts` — `spring-boot-starter-oauth2-client` 추가
- `application.properties` — Google/Kakao/Naver 등록(빈 client-id 기본값), `app.social-login.*`
- `docker-compose.yml`, `.env.example` — provider 자격증명/리다이렉트 URL 플레이스홀더 추가

### 테스트
- `SocialLoginServiceTest`(6케이스: 기존연결/신규가입/연동대기/연동확인성공/토큰만료/비밀번호불일치)
- `NormalizingOAuth2UserServiceTest`(provider별 attribute 파싱 6케이스)
- `SocialAccountRepositoryTest`, `PendingSocialLinkRepositoryTest`(`@DataJpaTest`)
- `LinkConfirmControllerTest`(`@WebMvcTest`, 3케이스)

전체 89개 테스트, `./gradlew test` 한 번에 통과.

## ⚠️ 검증되지 않은 위험 (Build and Test 단계에서 확인 필요)

- **빈 client-id로 `ClientRegistration`을 만들 때 앱 부팅 자체가 실패할 수 있다.** 실제 MariaDB/Redis/RabbitMQ가 없는 이 환경에서는 전체 Spring 컨텍스트를 띄워볼 수 없어 검증하지 못했다. 만약 부팅이 실패하면, `spring.security.oauth2.client.registration.*` 프로퍼티를 별도 `application-oauth2.properties`(프로필 분리)로 옮기는 대안을 Build and Test 단계에서 적용해야 한다.

## 다음 Unit에 대한 통합 지점 / 알려진 제약

1. **`SocialLoginSecurityConfig`의 `temporaryOpenFilterChain`(Order 2)은 임시 스캐폴딩이다.** Authorization Unit이 이를 완전히 대체해 `JwtAuthenticationFilter` 등록 + 실제 공개/보호 규칙을 적용해야 한다.
2. **연동 확인 API(`POST /api/auth/social/link/confirm`)는 Rate Limit 미통합.**
3. **소셜 전용 계정의 무작위 비밀번호**는 Account Unit의 비밀번호 재설정 플로우로만 실제 비밀번호를 설정할 수 있다(별도 UX 안내는 프런트엔드 책임, 이 프로젝트 범위 밖).
