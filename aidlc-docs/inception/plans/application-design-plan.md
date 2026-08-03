# Application Design Plan

**Role**: Software Architect
**Scope**: High-level component identification and service layer design for `auth-service` (auth/authz + JWT lifecycle + social login + rate-limit/IP-block + RBAC). The API Gateway is a separate project and out of scope here (decided 2026-07-31).
**Inputs**: `aidlc-docs/inception/requirements/requirements.md`, `aidlc-docs/inception/user-stories/stories.md`, `aidlc-docs/inception/user-stories/personas.md`

## Execution Checklist

- [x] Step A: Resolve architectural decisions below (gate — needs user answers)
- [x] Step B: Generate `components.md`
- [x] Step C: Generate `component-methods.md`
- [x] Step D: Generate `services.md`
- [x] Step E: Generate `component-dependency.md`
- [x] Step F: Generate consolidated `application-design.md`

## Proposed Component Breakdown (for confirmation in Q6 below)

| Component | Responsibility |
|---|---|
| `AccountComponent` | Signup, password hashing/verification, email verification, password reset |
| `SocialLoginComponent` | OAuth2 authorization-code exchange for Kakao/Naver/Google, account creation/linking |
| `TokenComponent` | JWT issuance, validation, refresh + rotation, reuse-detection, blacklist management |
| `RateLimitComponent` | Per-IP and per-account limit counters, auto IP block/unblock (Redis-backed) |
| `AuthorizationComponent` | Role (RBAC) checks for this service's own protected endpoints (e.g., admin user management); issues role claims in JWTs for the separately-built API Gateway / other services to enforce their own checks |

One orchestrating service is anticipated (`AuthService`) — finalized in Step D based on answers below. (`GatewayRoutingComponent`/`GatewayService` removed 2026-07-31 — API Gateway is a separate project, out of scope.)

## Architectural Questions (GATE — answer before Step B proceeds)

### Question 1: 게이트웨이 구현 방식과 런타임 통일성 — **N/A (2026-07-31, 범위 제거)**
~~현재 스켈레톤은 `spring-boot-starter-webmvc`(서블릿/블로킹)로 생성돼 있습니다. 반면 이 서비스는 모든 클라이언트 요청을 프록시하는 API 게이트웨이 역할도 겸합니다(FR-07). 게이트웨이 라우팅 레이어를 어떻게 구현할까요?~~

**대화에서 확정**: "게이트웨이는 따로 만들래. 인증/인가 api만 구현해줘." — API 게이트웨이는 별도 프로젝트로 구축되며 이 저장소 범위 밖. 이 서비스는 기존 `spring-boot-starter-webmvc`(blocking) 스택을 그대로 유지하며, 리액티브 런타임/게이트웨이 프록시 로직은 구현하지 않는다. (아래 Clarification Round 1도 이 결정으로 함께 해소됨.)

### Question 2: 소셜 로그인 시 동일 이메일 계정 연동 정책
Stories(US-201)에서 Application Design 단계로 명시적으로 넘겨진 결정입니다. 이메일/비밀번호로 이미 가입된 이메일과 동일한 이메일의 소셜 계정으로 로그인하면 어떻게 처리할까요?

A) 자동 연동 — 동일 이메일이면 소셜 계정을 기존 로컬 계정에 자동으로 연결

B) 별도 계정 생성 — 이메일이 같아도 소셜 로그인은 항상 새 계정으로 취급 (연동 안 함)

C) 사용자 확인 후 연동 — 기존 계정 발견 시 사용자에게 연동 여부를 확인받고 진행 (예: 기존 계정 비밀번호 재확인 등 추가 검증)

X) Other (please describe after [Answer]: tag below)

[Answer]: C

### Question 3: 게이트웨이 라우팅 규칙 등록 방식 — **N/A (2026-07-31, 범위 제거)**
~~US-602(신규 백엔드 서비스 라우팅 등록 확장성)는 "코드 변경 없이" 라우팅 추가를 요구합니다. 라우팅 규칙을 어떻게 관리할까요?~~

US-601/US-602(게이트웨이 라우팅 스토리)는 삭제됨 — 게이트웨이가 별도 프로젝트이므로 이 서비스는 라우팅 규칙을 관리하지 않는다.

### Question 4: 프록시 라우팅 시 역할(Role) 요구사항 검증 위치 — **N/A (2026-07-31, 범위 제거), 대체 결정으로 해소**
~~A) 라우팅 규칙 자체에 필요 역할을 함께 선언하고, 게이트웨이가 프록시 전에 역할 검증까지 수행 / B) 게이트웨이는 인증만 검증 후 프록시, 인가는 각 백엔드 서비스가 자체 수행~~

**대체 결정**: 이 서비스가 발급하는 JWT에 role 클레임을 포함시키고, 별도 게이트웨이 및 각 백엔드 서비스가 그 클레임으로 자체 인가를 수행한다(사실상 원안의 B와 동일한 신뢰 모델이지만, 라우팅 자체가 이 서비스 범위 밖이므로 게이트웨이의 구체적 검증 방식은 그 프로젝트가 결정).

### Question 5: 이메일 발송(인증 메일/비밀번호 재설정 메일) 처리 주체
현재 워크스페이스에는 다른 서비스가 존재하지 않습니다(requirements.md 참고).

A) 이 서비스가 직접 SMTP/이메일 발송 클라이언트를 내장하여 처리 (가장 단순, 지금 구조에 맞음)

B) 메시지 큐에 이벤트만 발행 — 실제 발송은 향후 별도 Notification 서비스가 담당 (지금은 발행 채널만 마련, consumer는 미구현 상태로 남김)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 6: 컴포넌트 구성 확인 — **개정 (2026-07-31)**
게이트웨이 범위 제거에 따라 `GatewayRoutingComponent`를 제외한 5개 컴포넌트(AccountComponent / SocialLoginComponent / TokenComponent / RateLimitComponent / AuthorizationComponent) 구성으로 진행합니다. 원래 [Answer]: A(제안된 구성 그대로)의 취지를 유지하되 게이트웨이 관련 컴포넌트만 제거한 것이므로, 별도 재확인 없이 이 구성으로 Step B를 진행합니다. Application Design 완료 메시지에서 이견이 있으면 "Request Changes"로 조정 가능합니다.

[Answer]: A (5개 컴포넌트로 확정, GatewayRoutingComponent 제외)

---

## Clarification Questions (Round 1) — **해소됨 (2026-07-31)**

Q1 답변(A)의 "별도 리액티브 런타임" 해석 문제(하이브리드 단일 앱 vs 별도 배포 단위)는, 대화에서 사용자가 "게이트웨이는 따로 만들래. 인증/인가 api만 구현해줘."라고 명확히 확정하면서 자연스럽게 해소되었습니다. 게이트웨이는 이 저장소와 별개의 프로젝트로 구축되며(옵션 B와 유사하되, 이 저장소 안에 멀티모듈로 두는 것도 아니고 완전히 다른 프로젝트), 이 서비스는 기존 blocking Spring MVC 스택을 유지한 채 인증/인가 API만 구현합니다.

[Answer]: 해소됨 — 게이트웨이 별도 프로젝트, 이 서비스는 인증/인가 API 전용
