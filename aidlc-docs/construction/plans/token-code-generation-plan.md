# Code Generation Plan — Unit: Token

**워크스페이스 루트**: `C:\IdeaProjects\e-commerce-be-auth` (`aidlc-state.md` 참고)
**프로젝트 구조**: Greenfield, 단일 Gradle 모듈, 패키지로 Unit 구분 (`unit-of-work.md` Q3:A) — 이 Unit은 `com.ecommerce.auth.token` 패키지
**구현 스토리**: US-301, US-302, US-303, US-304
**의존 Unit**: 없음 (Token은 기반 Unit)
**이 Unit이 노출하는 계약** (다른 Unit이 나중에 호출):
- `TokenIssuanceService.issue(accountId, role)` — Account/SocialLogin Unit이 로그인 성공 시 호출
- `TokenIssuanceService.revokeAllForAccount(accountId)` — Account Unit이 비밀번호 변경 시 호출
- REST: `POST /api/auth/token/refresh`, `POST /api/auth/logout`, `POST /internal/tokens/validate`

**범위 경계 참고**: `JwtAuthenticationFilter`는 이 Unit이 생성하지만, 아직 `SecurityFilterChain` Bean에 등록하지 않는다 — 공개 엔드포인트 목록/역할 기반 인가 규칙은 Authorization Unit(Unit 5)의 책임이므로, 그 Unit의 Code Generation에서 필터 등록과 `SecurityFilterChain` 구성을 완성한다 (`nfr-design/logical-components.md`, `unit-of-work.md` 경계 근거).

## Steps

- [x] Step 1: Project Structure Setup — `build.gradle.kts`에 의존성 추가(jjwt, spring-boot-starter-data-redis, Flyway+flyway-mysql), 패키지 스켈레톤 생성
- [x] Step 2: Business Logic Generation — `RefreshToken` 엔티티, `RefreshTokenStatus` enum, `JwtProvider`(발급/검증), `TokenIssuanceService`(6개 절차: issue/refresh/reuse-detect/logout/validate/revokeAllForAccount)
- [x] Step 3: Business Logic Unit Testing — `TokenIssuanceServiceTest`(JUnit5+Mockito, 6개 절차 각각의 정상/예외 경로), `JwtProviderTest` + jqwik PBT round-trip 테스트(클레임 encode/decode, PBT-02)
- [x] Step 4: Business Logic Summary — 문서화
- [x] Step 5: API Layer Generation — `TokenController`(refresh, logout), `TokenValidationController`(internal validate), 요청/응답 DTO, 예외 → HTTP 상태 매핑(`@RestControllerAdvice`)
- [x] Step 6: API Layer Unit Testing — `@WebMvcTest` 기반 컨트롤러 테스트
- [x] Step 7: API Layer Summary — 문서화
- [x] Step 8: Repository Layer Generation — `RefreshTokenRepository`(Spring Data JPA), `TokenBlacklistStore`(RedisTemplate 래퍼, 단발 재시도 후 fail-closed)
- [x] Step 9: Repository Layer Unit Testing — `RefreshTokenRepositoryTest`(`@DataJpaTest`), `TokenBlacklistStoreTest`(Mockito로 RedisTemplate mock, 재시도/fail-closed 경로 검증)
- [x] Step 10: Repository Layer Summary — 문서화
- [x] Step 11: Database Migration Script — Flyway `V1__create_refresh_token_table.sql`
- [x] Step 12: Documentation Generation — `aidlc-docs/construction/token/code/code-summary.md`(생성된 파일 목록, API 계약, 다음 Unit이 알아야 할 통합 지점)
- [x] Step 13: Deployment Artifacts — `docker-compose.yml`(auth-service+mariadb+redis, `shared-infrastructure.md` 반영), `application.yml`(datasource/redis/jwt 설정, 환경변수 참조)

## Testable Properties (PBT-01, 참고용 — Partial 모드에서는 advisory)

| 대상 | 속성 카테고리 | 테스트 |
|---|---|---|
| JWT 클레임 encode/decode | Round-trip | `JwtProvider.parse(JwtProvider.issue(claims)) == claims` (PBT-02, 강제) |
