# Code Generation Plan — Unit: SocialLogin

**패키지**: `com.ecommerce.auth.sociallogin`
**구현 스토리**: US-201, US-202, US-203
**의존 Unit**: Account(완료됨, 3개 신규 메서드 추가 필요), Token(완료됨, `issue()` 재사용)

**이 Unit이 노출하는 것**:
- Spring Security 내장 `/oauth2/authorization/{kakao,naver,google}`, `/login/oauth2/code/{kakao,naver,google}`
- `POST /api/auth/social/link/confirm` (연동 확인)

## ⚠️ SecurityConfig에 대한 특별 참고

OAuth2 로그인이 동작하려면 `.oauth2Login(...)`이 설정된 `SecurityFilterChain`이 **지금 존재해야** 합니다(Authorization Unit까지 미룰 수 없음 — 안 그러면 이 Unit 자체가 동작 불가). 다만 전체 서비스의 최종 인가 규칙(공개 엔드포인트 목록, 역할 검증, `JwtAuthenticationFilter` 등록)은 여전히 Authorization Unit 책임입니다. 이를 위해 **두 개의 `SecurityFilterChain` Bean**을 둡니다:

1. `@Order(1)` — `/oauth2/**`, `/login/**` 경로 전용, `oauth2Login()` 설정 (이 Unit의 정식 산출물, 영구적)
2. `@Order(2)` — 나머지 모든 경로에 대한 **임시 permitAll + CSRF 비활성화** catch-all (Authorization Unit이 도착하면 **완전히 대체**해야 하는 스캐폴딩 — 코드에 명확히 주석으로 표시). 이게 없으면 Build and Test 단계 전까지 Account/Token API 자체가 Spring Security 기본 동작(전체 인증 요구)에 막혀 테스트/사용이 불가능해짐.

## Steps

- [x] Step 0: Account Unit에 메서드 추가(계획 3개 + 구현 중 발견한 `findById` 1개 추가) — `findByEmail`, `findById`, `createSociallyVerifiedAccount`, `verifyPassword`. 기존 `AccountServiceTest` 재실행으로 회귀 없음 확인
- [x] Step 1: Project Structure Setup — `build.gradle.kts`에 `spring-boot-starter-oauth2-client` 추가
- [x] Step 2: Business Logic Generation — `SocialProvider` enum, `SocialAccount`/`PendingSocialLink` 엔티티, `SocialLoginProperties`, `NormalizingOAuth2UserService`, `SocialLoginService`(로그인/가입/연동판단/연동확인), 예외 클래스
- [x] Step 3: Business Logic Unit Testing — `SocialLoginServiceTest`(6케이스), `NormalizingOAuth2UserServiceTest`(6케이스)
- [x] Step 4: Business Logic Summary
- [x] Step 5: API/Security Layer Generation — `SocialLoginSuccessHandler`, `SocialLoginFailureHandler`, `SocialLoginSecurityConfig`(위 두 필터체인), `LinkConfirmController`(연동 확인 API), DTO, 예외 핸들러
- [x] Step 6: API Layer Unit Testing — `@WebMvcTest`(연동 확인 컨트롤러, 3케이스)
- [x] Step 7: API Layer Summary
- [x] Step 8: Repository Layer Generation — `SocialAccountRepository`, `PendingSocialLinkRepository`
- [x] Step 9: Repository Layer Unit Testing — `@DataJpaTest`
- [x] Step 10: Repository Layer Summary
- [x] Step 11: Database Migration Script — Flyway `V3__create_social_login_tables.sql`
- [x] Step 12: Documentation Generation — `aidlc-docs/construction/sociallogin/code/code-summary.md`
- [x] Step 13: Deployment Artifacts — `application.properties`에 provider 등록/리다이렉트 URL/연동TTL 설정 추가, `docker-compose.yml`/`.env.example` 갱신
