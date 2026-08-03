# Functional Design Plan — Unit: SocialLogin

**Unit 책임**: Kakao/Naver/Google OAuth2 로그인, 계정 생성/연동 (`unit-of-work.md` Unit 3)
**관련 스토리**: US-201(카카오), US-202(네이버), US-203(구글)
**의존 Unit**: Account(계정 조회/생성, 완료됨), Token(로그인 성공 시 토큰 발급, 완료됨)
**입력 문서**: `requirements.md`(FR-05), `stories.md`, `application-design/components.md`, `application-design-plan.md`(Q2:C — 사용자 확인 후 연동)

## Execution Checklist

- [x] Step A: Resolve business logic questions below (gate)
- [x] Step B: Generate `business-logic-model.md`
- [x] Step C: Generate `business-rules.md`
- [x] Step D: Generate `domain-entities.md`

## Questions (GATE)

### Question 1: OAuth2 통합 방식
Spring Security의 `oauth2-client`(OAuth2Login 전체 필터 체인, 브라우저 세션/리다이렉트 흐름에 최적화)를 쓸지, 아니면 이 서비스가 API 전용이라는 점(component-methods.md의 `startAuthorization`/`handleCallback` 메서드 계약과 일치)을 고려해 각 provider의 토큰/사용자정보 엔드포인트를 `RestClient`로 직접 호출하는 경량 구현을 쓸지 결정이 필요합니다.

A) `RestClient` 기반 수동 구현 — provider별 `SocialProviderClient` 어댑터(카카오/네이버/구글)가 각각 인가코드→액세스토큰 교환, 사용자정보 조회를 직접 호출. Spring Security의 OAuth2Login 필터 체인(세션 기반)을 쓰지 않음 — 이 서비스는 세션이 아니라 JWT 기반 API이므로 더 자연스럽게 맞음.

B) Spring Security `oauth2-client` 도입 — Google은 기본 지원, Kakao/Naver는 커스텀 provider 등록 필요. 대신 CSRF/state 검증 등 일부를 프레임워크가 대신 처리.

X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 2: 계정 연동 확인(Q2:C) 구체적 흐름
Application Design에서 "사용자 확인 후 연동"으로 결정했습니다. 구체적으로 어떻게 확인받을까요?

A) 짧은 TTL의 "연동 확인 토큰" 발급 — 소셜 콜백에서 동일 이메일 기존 계정을 발견하면 로그인 대신 `{linkToken, maskedEmail}`을 반환. 클라이언트가 별도 "연동 확인" 화면에서 기존 계정 비밀번호를 입력받아 `POST .../link/confirm {linkToken, existingPassword}` 호출 → 비밀번호 확인 성공 시 연동 완료 + 토큰 발급.

B) 다른 방식 — [Answer]: 뒤에 설명해주세요

[Answer]: A

## 사전 결정 사항 (질문 없이 적용)

- **실제 Provider 자격증명(client id/secret)**: 이번 단계에서는 실제 값이 없음 — 환경변수로 주입하는 설정 구조만 만들고, 값은 비워둔다(`.env.example`에 플레이스홀더 추가). 로컬 테스트는 각 provider 클라이언트를 목/스텁으로 검증한다.
- **필요한 사용자 정보**: provider별 응답에서 최소 `providerUserId`, `email`만 추출한다(닉네임/프로필 사진 등은 이번 범위 밖).
- **신규 계정 생성 시 상태**: 소셜 로그인으로 신규 생성된 계정은 이메일 소유가 provider에 의해 이미 확인된 것으로 간주해 즉시 `ACTIVE`로 생성한다(이메일 인증 절차 건너뜀) — Account Unit의 `AccountStatus.ACTIVE`를 바로 사용, 이메일 인증 토큰 발급 없음.
