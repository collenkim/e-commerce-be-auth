# Application Design Plan

**Role**: Software Architect
**Scope**: High-level component identification and service layer design for `auth-service` (auth/authz + JWT lifecycle + social login + rate-limit/IP-block + RBAC + API-gateway routing).
**Inputs**: `aidlc-docs/inception/requirements/requirements.md`, `aidlc-docs/inception/user-stories/stories.md`, `aidlc-docs/inception/user-stories/personas.md`

## Execution Checklist

- [ ] Step A: Resolve architectural decisions below (gate — needs user answers)
- [ ] Step B: Generate `components.md`
- [ ] Step C: Generate `component-methods.md`
- [ ] Step D: Generate `services.md`
- [ ] Step E: Generate `component-dependency.md`
- [ ] Step F: Generate consolidated `application-design.md`

## Proposed Component Breakdown (for confirmation in Q6 below)

| Component | Responsibility |
|---|---|
| `AccountComponent` | Signup, password hashing/verification, email verification, password reset |
| `SocialLoginComponent` | OAuth2 authorization-code exchange for Kakao/Naver/Google, account creation/linking |
| `TokenComponent` | JWT issuance, validation, refresh + rotation, reuse-detection, blacklist management |
| `RateLimitComponent` | Per-IP and per-account limit counters, auto IP block/unblock (Redis-backed) |
| `AuthorizationComponent` | Role (RBAC) checks against required-role metadata for a given request/route |
| `GatewayRoutingComponent` | Reverse-proxy routing to backend microservices, route-table management |

Two orchestrating services are anticipated (`AuthService`, `GatewayService`) — finalized in Step D based on answers below.

## Architectural Questions (GATE — answer before Step B proceeds)

### Question 1: 게이트웨이 구현 방식과 런타임 통일성
현재 스켈레톤은 `spring-boot-starter-webmvc`(서블릿/블로킹)로 생성돼 있습니다. 반면 이 서비스는 모든 클라이언트 요청을 프록시하는 API 게이트웨이 역할도 겸합니다(FR-07). 게이트웨이 라우팅 레이어를 어떻게 구현할까요?

A) Spring Cloud Gateway 도입 (WebFlux/Netty 기반 리액티브) — 게이트웨이 라우팅 레이어만 별도 리액티브 런타임으로 분리하고, Auth 도메인 로직(회원가입/로그인/토큰 등)은 기존처럼 blocking으로 유지

B) 기존 Spring MVC(blocking) 스택을 그대로 유지 — 서블릿 필터/인터셉터 + `RestClient`(블로킹) 기반 동기 프록시로 게이트웨이 라우팅 구현 (런타임을 하나로 통일, 별도 리액티브 스택 도입 안 함)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 소셜 로그인 시 동일 이메일 계정 연동 정책
Stories(US-201)에서 Application Design 단계로 명시적으로 넘겨진 결정입니다. 이메일/비밀번호로 이미 가입된 이메일과 동일한 이메일의 소셜 계정으로 로그인하면 어떻게 처리할까요?

A) 자동 연동 — 동일 이메일이면 소셜 계정을 기존 로컬 계정에 자동으로 연결

B) 별도 계정 생성 — 이메일이 같아도 소셜 로그인은 항상 새 계정으로 취급 (연동 안 함)

C) 사용자 확인 후 연동 — 기존 계정 발견 시 사용자에게 연동 여부를 확인받고 진행 (예: 기존 계정 비밀번호 재확인 등 추가 검증)

X) Other (please describe after [Answer]: tag below)

[Answer]: C

### Question 3: 게이트웨이 라우팅 규칙 등록 방식
US-602(신규 백엔드 서비스 라우팅 등록 확장성)는 "코드 변경 없이" 라우팅 추가를 요구합니다. 라우팅 규칙을 어떻게 관리할까요?

A) 정적 설정 파일 기반 (application.yml 등, 설정 리로드로 라우팅 추가/변경 — 가장 단순, US-602 요구사항 충족)

B) DB 기반 동적 라우팅 테이블 (재시작 없이 런타임에 라우팅 추가/변경, 별도 관리 API 필요)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: 프록시 라우팅 시 역할(Role) 요구사항 검증 위치
A) 라우팅 규칙 자체에 필요 역할을 함께 선언(예: `/seller/** → SELLER 역할 필요`)하고, 게이트웨이가 프록시 전에 역할 검증까지 수행

B) 게이트웨이는 토큰 유효성(인증)만 검증 후 프록시하고, 역할 기반 인가(인가)는 각 백엔드 서비스가 전달받은 role 클레임으로 자체 수행

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: 이메일 발송(인증 메일/비밀번호 재설정 메일) 처리 주체
현재 워크스페이스에는 다른 서비스가 존재하지 않습니다(requirements.md 참고).

A) 이 서비스가 직접 SMTP/이메일 발송 클라이언트를 내장하여 처리 (가장 단순, 지금 구조에 맞음)

B) 메시지 큐에 이벤트만 발행 — 실제 발송은 향후 별도 Notification 서비스가 담당 (지금은 발행 채널만 마련, consumer는 미구현 상태로 남김)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 6: 컴포넌트 구성 확인
위 "Proposed Component Breakdown" 표의 6개 컴포넌트(AccountComponent / SocialLoginComponent / TokenComponent / RateLimitComponent / AuthorizationComponent / GatewayRoutingComponent) 구성이 적절한가요?

A) 제안된 구성 그대로 진행

B) 조정 필요 — [Answer]: 뒤에 구체적으로 설명해주세요 (예: 특정 컴포넌트 합치기/쪼개기)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Clarification Questions (Round 1)

Q1 답변(A)이 두 가지로 해석될 수 있어 확인이 필요합니다: "게이트웨이 라우팅 레이어만 별도 리액티브 런타임으로 분리"

### Clarification 1: "별도 리액티브 런타임"의 정확한 의미

A) **단일 애플리케이션, 하이브리드 실행 모델** — `e-commerce-be-auth` 저장소 전체를 Spring WebFlux 기반 애플리케이션 하나로 전환하고, DB 접근이 필요한 Auth 도메인 로직(blocking JDBC/JPA)은 `Schedulers.boundedElastic()` 등으로 감싸 리액티브 파이프라인 안에서 격리. 배포 단위는 지금처럼 하나(Spring Boot 앱 1개, 프로세스 1개).

B) **물리적으로 별도 배포 단위** — 게이트웨이 라우팅과 Auth 도메인 로직을 별도의 Spring Boot 애플리케이션(별도 프로세스/모듈, 예: `gateway` + `auth`)으로 분리. 이 경우 지금까지 "standalone 단일 서비스"로 진행해온 프로젝트 범위를 벗어나며, 사실상 두 번째 서비스를 새로 만드는 것과 같아 프로젝트 구조(멀티모듈 전환 등)부터 다시 논의가 필요합니다.

X) Other (please describe after [Answer]: tag below)

[Answer]:
