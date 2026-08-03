# Cross-Unit Integration Points (누적 추적)

완료된 Unit들의 `code-summary.md`에 흩어져 있던 "알려진 제약 / 다른 Unit이 해야 할 일"을 한 곳에 모은다. 각 Unit의 Code Generation이 끝날 때마다 이 파일에 항목을 추가한다. 담당 Unit이 실제로 처리하면 항목을 체크하고 처리한 Unit/커밋 맥락을 남긴다(삭제하지 않음 — 이력 추적).

## 프로젝트 정책: DB 스키마 관리 (2026-08-03 확정)

기능 개발이 진행 중인 동안(=아직 스키마가 확정/릴리스되지 않은 동안)에는 Flyway 마이그레이션을 **`V1__create_schema.sql` 파일 하나**로만 관리한다. 각 Unit이 Code Generation 때마다 새 버전 파일(`V1`, `V2`, `V3`...)을 추가하던 기존 방식은 폐기했다 — 순차 버전 분리는 **스키마가 확정된 이후, 운영 중인 DB에 안전하게 적용해야 하는 변경 이력**을 관리하기 위한 용도이지, 아직 아무도 참조하지 않는 개발 중 스키마를 위한 것이 아니다.

- Token/Account/SocialLogin Unit이 각각 만든 `V1__create_refresh_token_table.sql`/`V2__create_account_and_tokens.sql`/`V3__create_social_login_tables.sql`을 `V1__create_schema.sql` 하나로 통합했다(테이블/인덱스 내용은 100% 동일, 순서만 재배열).
- **앞으로 개발 중 스키마 변경은 이 `V1` 파일을 직접 고친다.** 스키마가 확정(예: 첫 배포/릴리스)된 이후부터 `V2`, `V3`...로 이력을 쌓기 시작한다.
- 이 변경으로 로컬 `mariadb-data` 볼륨(2026-08-03 인프라 통합 테스트에서 생성된 것)의 Flyway 이력이 새 체크섬과 맞지 않게 되어 볼륨을 삭제하고 재생성해 확인했다 — 로컬 테스트 데이터라 영향 없음.
- `V1__create_schema.sql`의 모든 테이블/컬럼에 `--` 설명 주석과 함께 MariaDB `COMMENT` 속성(테이블: `COMMENT = '...'`, 컬럼: 각 컬럼 정의 뒤 `COMMENT '...'`)을 추가했다 — `--` 주석은 소스 파일에만 존재하지만 `COMMENT` 속성은 DB 메타데이터에 실제로 저장되어 `SHOW FULL COLUMNS`/`information_schema.COLUMNS`/DB 클라이언트 툴에서 소스코드 없이도 확인 가능하다. 컬럼 코멘트 반영 후 재차 볼륨을 삭제/재생성해 `Successfully applied 1 migration` 및 `SHOW FULL COLUMNS`로 전 컬럼 코멘트 반영을 확인했다.

## API 문서화: springdoc-openapi(Swagger UI) 도입 (2026-08-03)

`org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0`을 추가해 `/swagger-ui/index.html`(UI)과 `/v3/api-docs`(OpenAPI 3.1 스펙)를 자동 노출한다. Spring Boot 4.0.7/Spring Framework 7 환경에서 정상 동작 확인(springdoc 2.x는 Spring Boot 3 대상이라 호환 안 됨 — 3.x 계열 사용).

- `AuthorizationSecurityConfig.PUBLIC_PATHS`에 `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`을 추가해 인증 없이 문서 조회가 가능하도록 했다(문서 자체는 민감정보가 아니므로 공개, 실제 API 호출은 각 엔드포인트의 기존 인증/인가 규칙을 그대로 따름).
- `docker compose up --build`로 실제 기동 후 `curl -o /dev/null -w '%{http_code}' /swagger-ui/index.html` → 200, `/v3/api-docs` → 전체 엔드포인트 스키마 정상 반환, `/api/admin/**`은 여전히 401(무인증) 확인 — PUBLIC_PATHS 변경이 다른 경로의 인가 규칙에 영향을 주지 않음을 검증.
- 프로덕션 배포 시 `springdoc.api-docs.enabled=false`/`springdoc.swagger-ui.enabled=false`로 비활성화 가능(부팅 로그에 안내 문구 출력됨) — 현재는 로컬/개발 단계라 활성 상태 유지, 별도 설정 없음.

## Authorization Unit(Unit 5)이 반드시 해야 할 일

- [x] `com.ecommerce.auth.token.security.JwtAuthenticationFilter`를 `SecurityFilterChain`에 등록 — **처리 완료 (Authorization Unit Code Generation, 2026-08-03)**: `AuthorizationSecurityConfig.finalSecurityFilterChain`
- [x] `SocialLoginSecurityConfig`의 `temporaryOpenFilterChain`을 완전히 대체 — **처리 완료**: 해당 Bean 삭제, `finalSecurityFilterChain`(Order 2)으로 대체. `oauth2LoginFilterChain`(Order 1)은 유지
- [x] 공개(permitAll) 엔드포인트 목록 확정 — **처리 완료**: `/api/auth/signup`, `/api/auth/login`, `/api/auth/email/verify`, `/api/auth/email/verify/resend`, `/api/auth/password-reset/request`, `/api/auth/password-reset/execute`, `/api/auth/token/refresh`, `/api/auth/logout`, `/internal/tokens/validate`, `/oauth2/**`, `/login/**`, `/api/auth/social/link/confirm`. 그 외 전부 인증 필요, `/api/admin/**`은 ADMIN 역할 추가 필요
- [x] `/api/auth/logout`, `/internal/tokens/validate` 보호 여부 확정 — **처리 완료**: 둘 다 permitAll(자체 토큰 파싱/내부망 전제로 이미 보호됨, 재확인 완료)
- [x] SELLER/ADMIN 승격 경로 부재 — **처리 완료**: `PATCH /api/admin/accounts/{accountId}/role`(ADMIN 전용) 신설, `AccountService.changeRole()` 추가

## RateLimit Unit(Unit 4)이 반드시 해야 할 일

- [x] 다음 엔드포인트들에 Rate Limit 필터 결합 여부 확정 (FR-08 "인증 엔드포인트" 해석) — **처리 완료 (RateLimit Unit Code Generation, 2026-07-31)**:
  - `/api/auth/signup`, `/api/auth/login` → **적용함** (IP 필터 + 계정 기준은 `AccountController`가 직접 호출)
  - `/api/auth/token/refresh`, `/api/auth/logout`, `/api/auth/email/verify/resend`, `/api/auth/password-reset/request`, `/oauth2/authorization/**`, `/api/auth/social/link/confirm` → **적용 안 함** (Functional Design Q1:A, 이번 범위에서 명시적으로 제외 — 필요 시 향후 별도 확장)

## Build and Test 단계에서 검증 필요

- [x] ~~**(SocialLogin)** 빈 client-id로 ... 애플리케이션 부팅이 실패하는지 확인 — 해소 (RateLimit Unit Code Generation, 2026-07-31)~~ **⚠️ 이 결론은 틀렸다 — 2026-08-03 실제 docker-compose 기동으로 정정.** `src/test/resources/application.properties`가 `client-id=test-client-id`(비-공백 더미 값)를 명시적으로 설정하고 있어서 `contextLoads()`는 애초에 "빈 client-id" 시나리오를 검증한 적이 없었다. 실제로는 **빈 client-id가 `OAuth2ClientProperties` 검증에서 부팅을 막는다**(`Client id of registration 'google' must not be empty`). 아래 "실제 인프라 통합 테스트 결과"에서 근본 수정 완료.
- [x] **(전체)** `docker-compose.yml`로 전체 스택이 **실제 인프라**로 기동하는지 — **완료 (2026-08-03, 아래 결과 참고)**

## 실제 인프라 통합 테스트 결과 (2026-08-03, `docker-compose up`)

`integration-test-instructions.md` 시나리오 1~3을 실제 MariaDB/Redis/RabbitMQ로 전부 수행. **과정에서 발견 및 수정한 버그 3건**(전부 지금까지 어떤 자동화 테스트도 잡아내지 못했던 것들 — H2/mock으로는 재현 불가능한 클래스):

1. **docker-compose 시작 순서 경합** — `auth-service`가 `depends_on`(컨테이너 생성 순서만 보장, 준비 상태는 안 봄)만으로는 MariaDB가 실제로 연결을 받을 준비가 되기 전에 시작해 `Connection refused`로 크래시. `mariadb`/`redis`/`rabbitmq`에 `healthcheck` 추가 + `depends_on: condition: service_healthy`로 수정. 완전히 새로 내린 스택(`docker compose down -v` 후 `up`)으로 재현 및 수정 확인.
2. **Flyway 마이그레이션이 한 번도 실행되지 않고 있었다** — `implementation("org.flywaydb:flyway-core")`만으로는 이 Spring Boot 4.0.7 버전에서 `FlywayAutoConfiguration`이 활성화되지 않는다(이 버전은 `org.springframework.boot:spring-boot-starter-flyway`라는 별도 모듈로 재구성됨 — webmvc-test/data-jpa-test 때와 동일한 패턴). 에러조차 없이 조용히 스킵되어 `V1~V3` 테이블이 하나도 생성되지 않은 채 앱이 "정상" 기동되고 있었다. `flyway-core` 직접 의존성을 `spring-boot-starter-flyway`로 교체해 수정, `SHOW TABLES`로 7개 테이블 생성 확인.
3. **(정정, 위 항목과 동일 건)** 빈 OAuth2 client-id가 실제로 부팅을 막는다 — `application.properties`/`docker-compose.yml` 양쪽에 비-공백 placeholder 기본값(`unconfigured-google-client-id` 등)을 둬서 수정. 소셜 로그인 기능 자체는 여전히 비활성(의도된 동작), 앱 부팅만 항상 성공하도록.

**수정 후 시나리오별 결과**:
- **시나리오 1 (Account↔Token)**: 가입 → (RabbitMQ 관리 API로 임시 큐를 만들어 실제 발행된 이메일 인증 토큰 원문을 확인 — 코드 변경 없이 검증) → 이메일 인증 → 로그인 → 토큰 갱신 → **갱신된(구) refresh token 재사용 시 401 재사용탐지 확인** → 로그아웃 → **로그아웃 후 access token이 실제로 블랙리스트에 걸려 `/internal/tokens/validate`가 `valid:false` 반환하는 것까지 확인**. 전부 성공.
- **시나리오 2 (RateLimit↔Account)**: 동일 계정 5회 로그인 실패 → 6번째 시도에서 `429 ACCOUNT_TEMPORARILY_LOCKED` 확인. Redis에서 `block:account:*` 키의 TTL(약 891초, 설정된 15분과 일치)까지 직접 확인.
- **시나리오 3 (Authorization↔Account)**: **이전까지 자동화 테스트로 검증 못 했던 부분을 전부 닫음** — 실제 유효한 ADMIN 토큰으로 역할 변경 204, 토큰 없이 401, **실제 유효한 일반 USER 토큰으로 403(`ACCESS_DENIED`)까지 확인**.
- 시나리오 4(SocialLogin, 실제 provider 자격증명 필요)는 미실행 — 사전 결정대로 이번 라운드에서는 생략.

## 프로세스 노트 (2026-08-03 갱신)

**"슬라이스/전체-컨텍스트 테스트가 다 통과해도 실제 인프라로 띄워보기 전엔 모른다."** 이번 라운드에서 발견한 3건 버그 중 어느 것도 `./gradlew test`(H2 + mock 기반, 116개 전부 통과 상태였음)로는 절대 잡을 수 없는 종류였다 — 진짜 Docker 이미지 빌드, 진짜 컨테이너 시작 순서, 진짜 Flyway/MariaDB 조합, 진짜(빈) 환경변수가 있어야만 드러났다. `integration-test-instructions.md`의 "실제 인프라로 기동해보기 전엔 완료로 간주하지 말 것"이라는 취지가 정확히 맞았다.

## 프로세스 노트: `ECommerceBeAuthApplicationTests.contextLoads()`를 회귀 검사로 활용

이 프로젝트의 기존 스켈레톤에 있던 `@SpringBootTest` 테스트가 전체 애플리케이션 컨텍스트를 실제로 띄운다(MariaDB는 테스트 프로퍼티에서 H2로 대체). RateLimit Unit에서 처음으로 이 테스트가 통과했고, 그 과정에서 슬라이스 테스트(`@WebMvcTest`/`@DataJpaTest`)로는 잡히지 않던 실버그(`RateLimitFilterConfig`가 존재하지 않는 `ObjectMapper` Bean을 주입받으려 한 것)를 발견했다. **앞으로 매 Unit Code Generation마다 `./gradlew test`를 돌릴 때 이 테스트가 계속 통과하는지 확인한다** — 전체 컨텍스트 조립 문제를 가장 빨리 잡아내는 수단이다.

## 발견 및 수정된 버그 (참고용 — 의도적 결정이 아니라 결함이었던 것들)

- **(Authorization Unit이 발견, Account Unit 코드 수정)** `EmailVerificationRequested`/`PasswordResetRequested` 이벤트가 RabbitMQ 기본 `SimpleMessageConverter`(Serializable만 지원)와 맞지 않아 실제 발행이 항상 실패하고 있었다 — 지금까지 이 경로를 실제로 실행하는 테스트가 없어서 발견되지 않았다. `SecurityFilterChainIntegrationTest`(전체 컨텍스트)가 처음으로 잡아냄. `JacksonJsonMessageConverter` 도입 + `EmailEventPublisher`의 catch 절을 `RuntimeException`으로 확장해 수정, 회귀 테스트(`EmailEventPublisherTest`) 추가.
- **(RateLimit Unit이 발견, RateLimit Unit 코드 수정)** `RateLimitFilterConfig`가 존재하지 않는 `ObjectMapper` Bean을 주입받으려 해 전체 컨텍스트 부팅이 실패했었다 — 슬라이스 테스트로는 못 잡던 문제. `RateLimitFilter`가 자체 `ObjectMapper`를 생성하도록 수정.

**교훈**: 슬라이스 테스트(`@WebMvcTest`, `@DataJpaTest`)는 서비스 간 실제 연결(메시지 브로커 직렬화, Bean 조립 등)을 검증하지 못한다. `SecurityFilterChainIntegrationTest`/`ECommerceBeAuthApplicationTests.contextLoads()` 같은 전체 컨텍스트 테스트를 계속 유지해야 하는 이유.

## 알려진 설계상 제약 (Unit 담당 없음, 참고용 — 의도적 결정)

- **(Token)** 재사용 탐지는 Refresh Token 패밀리만 즉시 무효화한다 — 이미 발급된 Access Token은 자체 만료(최대 15분)까지 유효할 수 있음.
- **(Token)** 역할(Role)은 Refresh Token 발급 시점에 고정되어 세션 내내 승계된다 — 세션 중 역할이 바뀌면 재로그인 필요. 관리자가 역할을 변경해도 동일하게 적용됨(Authorization Unit).
- **(Account)** RabbitMQ에 큐/바인딩이 없다 — exchange 발행까지만 구현, consumer(Notification 서비스)는 범위 밖.
- **(SocialLogin)** 소셜 전용 계정은 무작위 비밀번호 해시를 가진다 — 실제 비밀번호를 설정하려면 Account Unit의 비밀번호 재설정 플로우를 거쳐야 한다(프런트엔드에서 이 사실을 안내해야 함, 이 프로젝트 범위 밖).
- **(Authorization)** 관리자가 자기 자신의 역할을 낮춰 스스로를 잠글 수 있다 — 특별한 방지 로직 없음(단순성 우선, 낮은 우선순위 리스크로 수용).

## 남은 미검증 항목

- [x] ~~`docker-compose.yml`로 전체 스택이 실제 인프라로 기동하는지~~ — **완료 (2026-08-03)**
- [x] ~~실제 유효한 USER 토큰으로 관리자 엔드포인트 호출 시 403이 반환되는지~~ — **완료 (2026-08-03)**
- [ ] 의존성 취약점 스캐너를 CI/CD에 실제로 연결(SECURITY-10, Authorization Unit NFR Requirements에서 non-blocking으로 이관됨) — 미실행.
- [ ] 시나리오 4(SocialLogin, 실제 Kakao/Naver/Google 앱 자격증명 필요) — 미실행, 실제 자격증명 없이는 검증 불가.
- [ ] 부하/성능 테스트 — `performance-test-instructions.md` 참고, 정의된 목표 자체가 없어 범위 밖으로 유지.
