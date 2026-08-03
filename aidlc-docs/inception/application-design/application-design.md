# Application Design (Consolidated)

**서비스**: `auth-service` — 인증/인가 API 전용. API 게이트웨이는 별도 프로젝트로 구축되며 이 저장소 범위 밖 (2026-07-31 결정, `application-design-plan.md` 참고).

**배포 단위**: 단일 Spring Boot 애플리케이션 (기존 blocking Spring MVC 스택 유지, 리액티브 런타임 도입 없음 — Q1).

## 컴포넌트 요약

| 컴포넌트 | 책임 | 상세 |
|---|---|---|
| `AccountComponent` | 회원가입/이메일 인증/비밀번호 재설정 | `components.md`, `component-methods.md` |
| `SocialLoginComponent` | Kakao/Naver/Google 소셜 로그인 | 상동 |
| `TokenComponent` | JWT 발급/회전/무효화/외부 검증 API | 상동 |
| `RateLimitComponent` | 인증 엔드포인트 Rate Limit / IP 자동 차단 | 상동 |
| `AuthorizationComponent` | 이 서비스 자체 보호 엔드포인트 역할 검증 | 상동 |

세부 메서드 시그니처는 `component-methods.md`, 오케스트레이션 흐름은 `services.md`(단일 `AuthService`), 의존성/통신 패턴은 `component-dependency.md` 참고.

## Application Design 단계에서 확정된 아키텍처 결정

| # | 결정 | 근거 |
|---|---|---|
| Q1 | 기존 blocking Spring MVC 스택 유지, 별도 리액티브 런타임 도입 안 함 | 게이트웨이 프록시 로직이 없으므로 리액티브 전환 불필요 |
| Q2 | 소셜 로그인 시 동일 이메일 발견 → 사용자 확인 후 연동 | 명시적 사용자 동의로 계정 탈취/오연동 리스크 최소화 |
| Q3, Q4 | N/A | 게이트웨이 라우팅/역할검증 위치 질문은 게이트웨이가 별도 프로젝트가 되며 소멸 |
| Q5 | 이메일 발송은 메시지 큐 이벤트 발행만 (consumer 미구현) | 향후 Notification 서비스 도입을 고려한 확장 포인트 마련, 지금은 최소 구현 |
| Q6 | 5개 컴포넌트 구성 확정 (`GatewayRoutingComponent` 제외) | API 게이트웨이 범위 제거에 따른 자연스러운 축소 |

## 게이트웨이 범위 제거의 실무적 함의

- 이 서비스는 요청을 다른 백엔드로 프록시하지 않는다. 클라이언트(또는 별도 게이트웨이)가 이 서비스의 REST API를 직접 호출한다.
- 다른 서비스/게이트웨이가 인가를 수행하려면 이 서비스가 발급한 JWT를 자체적으로 검증하거나, 이 서비스가 제공하는 `TokenComponent.validate()` API를 호출해야 한다. 이 계약(요청/응답 스키마)은 CONSTRUCTION 단계 Functional Design에서 구체화한다.
- `requirements.md`(FR-07, FR-12, NFR-07)와 `stories.md`(Epic 6 삭제, US-603 재정의)가 이 결정을 반영해 갱신되었다.

## Out of Scope (Application Design 단계 재확인)

- API 게이트웨이(라우팅/프록시) 구현 — 별도 프로젝트
- 상품 카탈로그 공개 접근 정책 — 상품 서비스/게이트웨이 책임
