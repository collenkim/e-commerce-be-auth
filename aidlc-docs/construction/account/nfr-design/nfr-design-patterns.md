# NFR Design Patterns — Unit: Account

## 코드 재사용 패턴: 공용 불투명 토큰 유틸리티 (Q1:A)

- `com.ecommerce.auth.shared.OpaqueTokenGenerator`를 신설한다 — 무작위 원문 생성(SecureRandom, Base64Url) + SHA-256 해시.
- **Code Generation 단계에서 Token Unit을 리팩터링**: `TokenIssuanceService`의 기존 `generateOpaqueToken()`/`hash()` private 메서드를 제거하고 이 공용 유틸리티를 사용하도록 변경한다. Token Unit의 기존 테스트(`TokenIssuanceServiceTest`)는 동작 변경이 없어야 하므로 리팩터링 후 재실행해 회귀가 없는지 확인한다.
- Account Unit(이메일 인증 토큰, 비밀번호 재설정 토큰)도 동일 유틸리티를 사용한다.

## 회복탄력성 패턴: 이메일 이벤트 발행

- `EmailEventPublisher`는 RabbitMQ 발행을 try-catch로 감싸고, 실패 시 로그만 남기고 예외를 전파하지 않는다.
- 재시도/서킷브레이커 라이브러리 도입 안 함 — Spring AMQP 기본 동작만 사용 (프로젝트 최소 회복탄력성 방침).

## 확장성/성능 패턴

- 무상태 애플리케이션, MariaDB 공유 인스턴스 — Token Unit과 동일한 원칙.
- BCrypt cost 12는 로그인 경로에 의도적 지연을 추가하지만, 목표 p99 100ms 대비 영향은 미미(수십 ms 수준)하다고 판단 — 실측 후 필요 시 조정.

## 보안 패턴

- Account Unit의 4개 공개 엔드포인트(가입/이메일인증/로그인/재설정)는 `JwtAuthenticationFilter`의 보호 대상이 아니다 — Authorization Unit이 향후 `SecurityFilterChain`에서 이 경로들을 `permitAll`로 명시해야 한다(통합 지점, Token Unit 코드 요약에 이미 기록된 패턴과 동일).
- 입력 검증(SECURITY-05)은 컨트롤러 DTO에 Bean Validation(`@Email`, `@NotBlank`, `@Size`)으로 적용한다.
