# Code Generation Summary — Unit: Token

## 생성된 파일

**패키지**: `com.ecommerce.auth.token` (+ 공통 타입 `com.ecommerce.auth.shared`)

### 비즈니스 로직
- `src/main/java/com/ecommerce/auth/shared/Role.java` — USER/SELLER/ADMIN (여러 Unit이 공유하는 타입)
- `src/main/java/com/ecommerce/auth/shared/ClockConfig.java` — 테스트 가능한 `Clock` Bean
- `src/main/java/com/ecommerce/auth/token/domain/RefreshToken.java`, `RefreshTokenStatus.java`
- `src/main/java/com/ecommerce/auth/token/JwtProvider.java`, `JwtProperties.java`
- `src/main/java/com/ecommerce/auth/token/TokenIssuanceService.java`
- `src/main/java/com/ecommerce/auth/token/dto/{AccessTokenClaims,TokenPair,TokenValidationResult}.java`
- `src/main/java/com/ecommerce/auth/token/exception/{InvalidTokenException,TokenReuseDetectedException}.java`

### API 레이어
- `src/main/java/com/ecommerce/auth/token/api/TokenController.java` — `POST /api/auth/token/refresh`, `POST /api/auth/logout`
- `src/main/java/com/ecommerce/auth/token/api/TokenValidationController.java` — `POST /internal/tokens/validate`
- `src/main/java/com/ecommerce/auth/token/api/TokenExceptionHandler.java`
- `src/main/java/com/ecommerce/auth/token/api/dto/*.java`

### 리포지토리 레이어
- `src/main/java/com/ecommerce/auth/token/repository/RefreshTokenRepository.java` (Spring Data JPA)
- `src/main/java/com/ecommerce/auth/token/repository/TokenBlacklistStore.java` (Redis, 단발 재시도 후 fail-closed)

### 보안 (미배선 — 아래 "다음 Unit에 대한 통합 지점" 참고)
- `src/main/java/com/ecommerce/auth/token/security/JwtAuthenticationFilter.java`

### DB 마이그레이션
- `src/main/resources/db/migration/V1__create_refresh_token_table.sql` (Flyway)

### 설정/빌드/배포
- `build.gradle.kts` — jjwt, spring-boot-starter-data-redis, Flyway(+mysql), jqwik, H2(테스트) 추가
- `src/main/resources/application.properties` — datasource/redis/jwt 설정(환경변수 참조)
- `src/test/resources/application.properties` — 테스트용 H2 설정
- `.env.example`, `.gitignore`(`.env` 추가)
- `Dockerfile`, `docker-compose.yml` (auth-service + mariadb + redis)

### 테스트
- `JwtProviderTest`, `JwtProviderPropertyTest`(jqwik, PBT-02 round-trip)
- `TokenIssuanceServiceTest` (10개 케이스: 발급/회전/재사용탐지/만료/로그아웃/검증/계정전체무효화)
- `TokenBlacklistStoreTest` (정상/재시도-성공/재시도-실패-fail closed/TTL<=0 스킵)
- `RefreshTokenRepositoryTest` (`@DataJpaTest`, H2)
- `TokenControllerTest`, `TokenValidationControllerTest` (`@WebMvcTest`)
- `JwtAuthenticationFilterTest`

전체 32개 테스트, `./gradlew test` 통과 확인.

## API 계약 (다른 Unit·외부 호출자용)

| 메서드/엔드포인트 | 설명 |
|---|---|
| `TokenIssuanceService.issue(UUID accountId, Role role)` | 로그인 성공 시 Account/SocialLogin Unit이 호출 |
| `TokenIssuanceService.revokeAllForAccount(UUID accountId)` | 비밀번호 변경 시 Account Unit이 호출 |
| `POST /api/auth/token/refresh` | `{refreshToken}` → `{accessToken, refreshToken}` |
| `POST /api/auth/logout` | Header `Authorization: Bearer <accessToken>` + `{refreshToken}` → 204 |
| `POST /internal/tokens/validate` | `{accessToken}` → `{valid, accountId, role}` — 별도 게이트웨이/다른 서비스용, 별도 인증 없음(내부망 전제) |

## 다음 Unit에 대한 통합 지점 / 알려진 제약

1. **`JwtAuthenticationFilter`는 아직 활성화되지 않았다.** 의도적으로 `@Component`가 아니다. **Authorization Unit(Unit 5)**이 이 클래스를 생성자 주입으로 받아 `SecurityFilterChain` Bean에 등록하고, 공개 엔드포인트 목록(permitAll)과 역할 요구사항을 정의해야 한다.
2. **역할(Role)은 Refresh Token 발급 시점에 고정되어 세션 내내 승계된다.** 세션 도중 역할이 바뀌면(예: 승격) 재로그인 전까지 반영되지 않는다 — Functional Design 단계에서 표시한 정책상 트레이드오프.
3. **재사용 탐지가 즉시 막는 것은 Refresh Token 패밀리뿐이다.** 그 패밀리로 이미 발급된 Access Token은 자체 만료(최대 15분)까지 유효할 수 있다 (`business-logic-model.md` 3번 절차 참고).
4. **Token의 refresh/logout 엔드포인트에 대한 Rate Limit 적용 여부는 미해결.** RateLimit Unit 설계 시 확정 필요 (`tech-stack-decisions.md` 참고).
5. **`Role` enum과 `Clock` Bean은 `com.ecommerce.auth.shared` 패키지로 분리**했다 — Account/SocialLogin/Authorization Unit도 이 타입들을 재사용하면 된다(중복 정의 금지).
