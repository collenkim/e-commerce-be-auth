# e-commerce-be-auth

이커머스 플랫폼의 **인증/인가(Authentication/Authorization) 전용 백엔드 서비스**입니다. 이메일/비밀번호 및 소셜(카카오·네이버·구글) 로그인, JWT 발급/검증, Rate Limit·IP 차단, 역할 기반 접근 제어(RBAC)를 담당합니다.

이 프로젝트는 AI-DLC(AI-assisted Development Life Cycle) 워크플로우로 개발되었습니다 — 설계 배경과 의사결정 이력은 [`aidlc-docs/`](aidlc-docs/)에 전부 기록되어 있습니다.

## 이 서비스의 역할과 범위

- **역할**: 플랫폼 전체가 신뢰할 수 있는 신원(accountId)과 권한(role)을 발급·검증하는 단일 창구. 다른 백엔드 서비스나 API 게이트웨이는 이 서비스가 발급한 JWT를 검증(또는 이 서비스의 검증 API를 호출)해 자체적으로 인가를 수행합니다.
- **API 게이트웨이가 아닙니다.** 원래 요구사항에는 게이트웨이 역할도 포함되어 있었으나, 개발 도중 "게이트웨이는 별도 프로젝트로 만든다"는 결정에 따라 이 저장소의 범위에서 제외되었습니다. 이 서비스는 요청을 다른 백엔드로 프록시하지 않습니다.
- **단일 배포 단위**입니다. 아래 5개 기능 영역은 논리적으로만 분리되어 있으며, 하나의 Spring Boot 애플리케이션으로 함께 빌드·배포됩니다.

## 주요 기능

| 영역 | 기능 |
|---|---|
| 계정 | 이메일/비밀번호 회원가입, 이메일 인증, 비밀번호 재설정 |
| 소셜 로그인 | 카카오·네이버·구글 OAuth2 로그인, 동일 이메일 기존 계정과의 연동(사용자 확인 후) |
| 토큰 | JWT Access/Refresh Token 발급, 회전(rotation), 재사용 탐지, 로그아웃 시 즉시 무효화, 외부 검증 API |
| 남용 방지 | 로그인/가입에 대한 IP·계정 기준 Rate Limit, 브루트포스 IP 자동 차단(TTL 기반 자동 해제) |
| 인가 | 역할 기반 접근 제어(USER/SELLER/ADMIN), 관리자용 역할 변경 API, 미인증 요청 일관된 차단 |

## API 개요

| 메서드/경로 | 인증 | 설명 |
|---|---|---|
| `POST /api/auth/signup` | 공개 | 회원가입 |
| `POST /api/auth/email/verify` | 공개 | 이메일 인증 |
| `POST /api/auth/email/verify/resend` | 공개 | 인증 메일 재발송 |
| `POST /api/auth/login` | 공개 | 로그인 → 토큰 발급 |
| `POST /api/auth/password-reset/request` / `.../execute` | 공개 | 비밀번호 재설정 요청/실행 |
| `POST /api/auth/token/refresh` | 공개(토큰 자체가 자격증명) | Refresh Token 회전 |
| `POST /api/auth/logout` | 공개(토큰 자체가 자격증명) | 로그아웃, 토큰 즉시 무효화 |
| `POST /internal/tokens/validate` | 공개(내부망 전제) | 다른 서비스/게이트웨이용 토큰 검증 |
| `GET /oauth2/authorization/{kakao\|naver\|google}` | 공개 | 소셜 로그인 시작 |
| `POST /api/auth/social/link/confirm` | 공개(연동 확인 토큰이 자격증명) | 소셜 계정과 기존 계정 연동 확인 |
| `PATCH /api/admin/accounts/{accountId}/role` | **ADMIN 역할 필요** | 계정 역할 변경 |

그 외 정의되지 않은 모든 경로는 유효한 Access Token(Bearer)이 필요합니다(deny-by-default).

## 기술 스택

- **언어/런타임**: Java 17, Spring Boot 4.0.7
- **빌드**: Gradle (Kotlin DSL)
- **데이터**: MariaDB(영속 데이터) + Flyway, Redis(블랙리스트/Rate Limit 카운터), RabbitMQ(이메일 발송 이벤트 발행)
- **보안**: Spring Security (JWT 자체 발급/검증, OAuth2 Client), BCrypt
- **API 문서**: springdoc-openapi(Swagger UI)
- **테스트**: JUnit 5, Mockito, jqwik(Property-Based Testing)

## 시작하기

### 로컬 전체 스택 실행 (Docker Compose)

```bash
cp .env.example .env
# .env에 DB_PASSWORD, JWT_HMAC_SECRET 값을 채운다 (아무 값이나 로컬에서는 무방)
docker compose up --build
```

`auth-service`(포트 8080), `mariadb`, `redis`, `rabbitmq`(관리 UI: `http://localhost:15672`, guest/guest)가 함께 기동됩니다. 카카오/네이버/구글 로그인은 실제 OAuth2 앱 자격증명을 `.env`에 채우기 전까지는 동작하지 않지만, 그 외 모든 기능은 정상 동작합니다.

### API 문서 (Swagger UI)

애플리케이션이 기동되면 인증 없이 바로 확인할 수 있습니다.

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html` — 브라우저에서 각 API를 직접 호출/테스트할 수 있습니다. 보호된 엔드포인트는 우측 상단 `Authorize` 버튼에 `Bearer <accessToken>`을 입력하면 됩니다.
- **OpenAPI 스펙(JSON)**: `http://localhost:8080/v3/api-docs`

### 빌드 및 테스트만 실행

```bash
./gradlew clean build   # 컴파일 + 116개 테스트 + jar 패키징
./gradlew test          # 테스트만
```

자세한 빌드/테스트/통합 테스트 절차는 [`aidlc-docs/construction/build-and-test/`](aidlc-docs/construction/build-and-test/)를 참고하세요.

### API 요청해보기 (curl)

전체 스택이 떠 있는 상태(`docker compose up`)를 기준으로 한 최소 예시입니다. 전체 시나리오(토큰 회전/재사용 탐지, Rate Limit, 관리자 권한 등)는 [`aidlc-docs/construction/build-and-test/integration-test-instructions.md`](aidlc-docs/construction/build-and-test/integration-test-instructions.md)를 참고하세요.

```bash
# 1. 회원가입
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password1"}'

# 2. 이메일 인증 토큰 확인 (실제 메일 발송 없음 - DB에서 직접 조회)
docker exec -it e-commerce-be-auth-mariadb-1 \
  mariadb -uauth_service -p"$DB_PASSWORD" auth_service \
  -e "SELECT token_hash FROM email_verification_token ORDER BY expires_at DESC LIMIT 1;"
# (해시만 저장되므로 실제 원문 토큰은 RabbitMQ에 발행된 이메일 이벤트에서 확인 - 관리 UI http://localhost:15672)

# 3. 로그인 → accessToken/refreshToken 발급
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password1"}'

# 4. 발급받은 accessToken으로 보호된 API 호출
curl -X POST http://localhost:8080/internal/tokens/validate \
  -H "Content-Type: application/json" \
  -d '{"accessToken":"<위에서 받은 accessToken>"}'
```

## 프로젝트 구조

단일 Gradle 모듈, 패키지로 기능 영역을 구분합니다(`src/main/java/com/ecommerce/auth/`):

```
account/       회원가입, 이메일 인증, 비밀번호 재설정
token/         JWT 발급/회전/검증, Refresh Token 관리
sociallogin/   카카오/네이버/구글 OAuth2 로그인, 계정 연동
ratelimit/     Rate Limit, IP/계정 자동 차단
authorization/ 최종 SecurityFilterChain, 관리자 역할 변경 API
shared/        여러 영역이 공유하는 타입(Role, OpaqueTokenGenerator 등)
```

DB 스키마는 `src/main/resources/db/migration/V1__create_schema.sql` 하나로 관리합니다. 기능 개발이 진행 중인 동안에는 이 파일을 직접 수정하고, 스키마가 확정(첫 릴리스)된 이후부터 `V2`, `V3`... 순차 마이그레이션으로 변경 이력을 관리합니다.

## 제약 사항 및 알려진 한계

- **API 게이트웨이 기능 없음** — 요청 프록시/라우팅은 이 서비스의 책임이 아닙니다(위 "역할과 범위" 참고).
- **역할(role) 변경은 재로그인 전까지 기존 세션에 반영되지 않습니다** — role은 Refresh Token 발급 시점에 클레임으로 고정되어 세션 내내 유지됩니다.
- **Refresh Token 재사용 탐지는 해당 토큰 패밀리만 즉시 무효화합니다** — 그 패밀리로 이미 발급된 Access Token은 자체 만료(최대 15분)까지 유효할 수 있습니다.
- **관리자가 자기 자신의 역할을 낮춰 스스로를 잠글 수 있습니다** — 별도 방지 로직이 없습니다.
- **이메일 발송은 실제로 이뤄지지 않습니다** — RabbitMQ에 이벤트를 발행하는 채널만 마련되어 있고, 이를 구독해 실제 메일을 보내는 Notification 서비스는 이 프로젝트 범위 밖입니다(개발/테스트 시에는 RabbitMQ 관리 UI 등으로 발행된 이벤트를 직접 확인해야 합니다).
- **소셜 로그인 전용 계정은 사용자가 알 수 없는 무작위 비밀번호를 가집니다** — 이메일/비밀번호 로그인을 쓰려면 "비밀번호 재설정" 플로우로 새 비밀번호를 설정해야 합니다.
- **관리자 승격을 위한 별도 부트스트랩 수단이 없습니다** — 최초 관리자 계정은 데이터베이스에 직접 `role`을 변경해야 합니다.
- **MFA(다중 인증) 미지원, Apple/Facebook 소셜 로그인 미지원**입니다.
- **성능/부하 테스트 목표가 정의되어 있지 않습니다** — Resiliency Baseline이 개발 초기에 의도적으로 최소화되었습니다(단일 리전, 별도 DR 없음).
- **의존성 취약점 스캐너가 CI/CD에 아직 연결되어 있지 않습니다.**
- **실제 클라우드 배포 대상이 정해지지 않았습니다** — 현재는 로컬 Docker Compose 환경만 검증되었습니다.

더 자세한 설계 배경, 각 기능 영역별 의사결정 근거, Unit 간 통합 지점은 [`aidlc-docs/construction/integration-points.md`](aidlc-docs/construction/integration-points.md)와 [`aidlc-docs/construction/`](aidlc-docs/construction/) 하위의 각 기능 영역 문서를 참고하세요.
