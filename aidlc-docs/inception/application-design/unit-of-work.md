# Unit of Work

**결정 근거**: `unit-of-work-plan.md` Q1:B(컴포넌트별 개별 Unit), Q2:A(단독/소규모 순차 개발), Q3:A(단일 Gradle 모듈, 패키지로 구분).

**배포 단위**: 단일 Spring Boot 애플리케이션(`auth-service`) — 아래 5개 Unit은 CONSTRUCTION 단계(Functional/NFR/Infra Design, Code Generation)를 반복 수행하기 위한 **설계/개발 단위**이며, 별도의 배포 단위나 별도 빌드 모듈이 아니다.

## Code Organization (Greenfield)

단일 Gradle 모듈, 컴포넌트별 패키지로 구분 (Q3:A):

```
com.ecommerce.auth
├── token          (Unit 1)
├── account        (Unit 2)
├── sociallogin    (Unit 3)
├── ratelimit      (Unit 4)
└── authorization  (Unit 5)
```

## Units

### Unit 1: Token
- **대응 컴포넌트**: `TokenComponent`
- **책임**: JWT 발급/회전/재사용탐지/무효화, 외부 검증(introspection) API
- **의존**: 없음 (다른 Unit에 의존하지 않는 기반 Unit)
- **관련 스토리**: US-301, US-302, US-303, US-304

### Unit 2: Account
- **대응 컴포넌트**: `AccountComponent`
- **책임**: 회원가입, 이메일 인증, 비밀번호 재설정
- **의존**: Unit 1(Token) — 비밀번호 재설정 시 기존 Refresh Token 전체 무효화 요청
- **관련 스토리**: US-101, US-102, US-104

### Unit 3: SocialLogin
- **대응 컴포넌트**: `SocialLoginComponent`
- **책임**: Kakao/Naver/Google OAuth2 로그인, 계정 생성/연동
- **의존**: Unit 2(Account) — 계정 조회/생성, Unit 1(Token) — 로그인 성공 시 토큰 발급
- **관련 스토리**: US-201, US-202, US-203

### Unit 4: RateLimit
- **대응 컴포넌트**: `RateLimitComponent`
- **책임**: 인증 엔드포인트 IP/계정 기준 Rate Limit, 자동 IP 차단/해제
- **의존**: 없음 (Redis 직접 사용, 다른 Unit의 API를 호출하지 않음 — 다른 Unit의 엔드포인트 앞단에 필터로 결합)
- **관련 스토리**: US-401, US-402, US-403, US-404, US-405

### Unit 5: Authorization
- **대응 컴포넌트**: `AuthorizationComponent`
- **책임**: 이 서비스 자체 보호 엔드포인트 역할(RBAC) 검증, 공개 엔드포인트 판단
- **의존**: Unit 1(Token) — 역할 클레임 조회
- **관련 스토리**: US-501, US-502, US-603

## 권장 구축 순서 (Q2:A, 단독/순차 개발 기준)

1. **Token** — 다른 Unit이 의존하는 기반 Unit
2. **Account** — Token에만 의존
3. **SocialLogin** — Account, Token에 의존
4. **RateLimit** — 독립적, 어느 시점에 진행해도 무방하나 인증 플로우(Account/SocialLogin) 완성 후 필터로 결합하는 것이 자연스러움
5. **Authorization** — Token에 의존, 다른 Unit들의 엔드포인트를 보호하므로 마지막에 배치
