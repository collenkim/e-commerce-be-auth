# Services

## AuthService (단일 오케스트레이팅 서비스)

Application Design 질문(Q1) 답변에 따라, 이 서비스는 단일 배포 단위(Spring Boot 앱 1개, 기존 blocking Spring MVC 스택)로 유지된다. 별도의 `GatewayService`는 없다 — API 게이트웨이는 별도 프로젝트다.

`AuthService`는 컨트롤러(REST API) 뒤에서 아래 컴포넌트들을 오케스트레이션하는 단일 서비스 계층이다. 컴포넌트별로 서비스를 쪼개지 않고 하나로 유지하는 이유: 컴포넌트 간 결합이 이미 강함(예: 로그인은 AccountComponent + RateLimitComponent + TokenComponent 세 곳을 항상 함께 호출), 서비스가 여러 개로 나뉘어도 별도 배포 단위가 아니므로 이점이 적음.

### 주요 오케스트레이션 흐름

| API 동작 | 참여 컴포넌트 (호출 순서) |
|---|---|
| 회원가입 | RateLimitComponent(IP 체크) → AccountComponent(가입) |
| 이메일 인증 | AccountComponent |
| 이메일/비밀번호 로그인 | RateLimitComponent(IP/계정 체크) → AccountComponent(자격 증명 검증) → TokenComponent(토큰 발급) |
| 소셜 로그인 콜백 | RateLimitComponent(IP 체크) → SocialLoginComponent(코드 교환/계정 연동, 필요 시 AccountComponent 호출) → TokenComponent(토큰 발급) |
| 토큰 갱신 | TokenComponent(회전 또는 재사용 탐지) |
| 로그아웃 | TokenComponent(무효화) |
| 비밀번호 재설정 | AccountComponent(재설정) → TokenComponent(기존 Refresh Token 전체 무효화) |
| 토큰 검증 (외부 호출자용) | TokenComponent(validate) — AuthorizationComponent를 거치지 않음 (외부 인가 판단은 호출자 책임) |
| 이 서비스 자체 보호 엔드포인트 접근 (예: 관리자 기능) | AuthorizationComponent(역할 검증) → 대상 기능 |

### 횡단 관심사 (Cross-cutting)

- **인증 필터**: 모든 요청 진입 시 AuthorizationComponent가 공개 엔드포인트 여부를 먼저 판단하고, 보호 대상이면 TokenComponent로 토큰을 검증한 뒤 역할을 확인한다.
- **Rate Limit 필터**: 인증 관련 엔드포인트(로그인/회원가입/비밀번호 재설정 요청 등)에만 RateLimitComponent가 적용된다.
