# Unit of Work Plan

**Role**: Architect/Decomposition Planner
**Inputs**: `aidlc-docs/inception/requirements/requirements.md`, `aidlc-docs/inception/user-stories/stories.md`, `aidlc-docs/inception/application-design/*`

## Execution Checklist

- [x] Step A: Resolve decomposition questions below (gate — needs user answers)
- [x] Step B: Generate `aidlc-docs/inception/application-design/unit-of-work.md`
- [x] Step C: Generate `aidlc-docs/inception/application-design/unit-of-work-dependency.md`
- [x] Step D: Generate `aidlc-docs/inception/application-design/unit-of-work-story-map.md`

## Context

Application Design(승인됨)에서 이 서비스는 **단일 배포 단위**(Spring Boot 앱 1개, 기존 blocking Spring MVC 스택)로 확정되었고, API 게이트웨이는 별도 프로젝트로 분리되어 이 저장소에는 존재하지 않습니다. 즉, 애초에 이 저장소를 "여러 서비스"로 쪼갤 이유(예: 게이트웨이 vs Auth 분리)가 사라졌습니다.

남은 질문은: 이 **하나의 배포 단위 안에서** Unit of Work를 몇 개로 나눌 것인가입니다. `units-generation.md` 정의상 모놀리스의 경우 "단일 유닛이 전체 애플리케이션(논리적 모듈 포함)을 대표"할 수도 있고, 기술적 이질성이 크면(예: OAuth2 연동 vs Redis 기반 Rate Limit vs JWT 암호화) 여러 유닛으로 나눠 유닛별로 다른 NFR/Infra Design 깊이를 줄 수도 있습니다.

## Decomposition Questions (GATE — answer before Step B proceeds)

### Question 1: Unit 개수/경계
5개 컴포넌트(AccountComponent, SocialLoginComponent, TokenComponent, RateLimitComponent, AuthorizationComponent)를 Unit of Work로 어떻게 묶을까요?

A) **단일 Unit** — `auth-service` 전체를 하나의 Unit of Work로 취급 (5개 컴포넌트는 그 안의 논리적 모듈). CONSTRUCTION 단계에서 Functional/NFR/Infra Design을 한 번만 수행하되, 컴포넌트별로 필요한 만큼 세부적으로 다룸. 단일 배포 단위이므로 가장 자연스러움.

B) **컴포넌트별 개별 Unit** (5개) — 각 컴포넌트를 독립된 Unit of Work로 취급해 CONSTRUCTION 단계를 컴포넌트마다 반복 수행 (예: RateLimitComponent는 Redis 중심 NFR/Infra를 별도로 깊이 다룸). 배포는 여전히 하나의 앱이지만, 설계 단계만 잘게 나눔.

C) **기술적 이질성 기준으로 소수 그룹화** — 예: (1) 계정/토큰/인가 [DB 중심], (2) 소셜로그인 [외부 OAuth2 연동], (3) Rate Limit [Redis/분산 상태] — 이렇게 2~3개 Unit으로 그룹화

X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 2: 팀/소유권 정렬
이 프로젝트에 여러 명이 병렬로 작업할 계획이 있나요? (Unit 경계를 팀 단위와 맞출지 판단하기 위함)

A) 아니오, 혼자(또는 매우 작은 팀이 순차적으로) 개발 — 병렬성보다 논리적 응집도를 기준으로 Unit을 나눠도 무방

B) 예, 여러 명/여러 스트림이 동시에 작업 예정 — Unit 경계가 곧 작업 분담 경계가 되어야 함

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 코드 구성 (Greenfield)
Unit 개수와 무관하게, 소스 코드 디렉토리 구조를 어떻게 조직할까요? (단일 Gradle 모듈 내 패키지 구조 vs Gradle 멀티모듈)

A) 단일 Gradle 모듈, 패키지로 구분 (예: `com.ecommerce.auth.account`, `.token`, `.sociallogin`, `.ratelimit`, `.authorization`) — 가장 단순, 단일 배포 단위와 자연스럽게 일치

B) Gradle 멀티모듈 (컴포넌트/Unit마다 별도 서브모듈) — 빌드 경계는 분리되지만 배포는 여전히 하나

X) Other (please describe after [Answer]: tag below)

[Answer]: A
