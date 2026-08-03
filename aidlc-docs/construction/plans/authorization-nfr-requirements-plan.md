# NFR Requirements Plan — Unit: Authorization

이전 4개 Unit의 공통 사항(수평 확장성, PBT=jqwik, 표준 커넥션 풀)을 상속한다. 이 Unit은 처음으로 전체 서비스의 `SecurityFilterChain`을 완성하는 Unit이므로 CORS 등 지금까지 다뤄지지 않은 항목을 짚는다.

## Execution Checklist

- [x] Step A: Resolve question below (gate)
- [x] Step B: Generate `nfr-requirements.md` (Security/PBT 준수 표 포함 — 이번엔 전체 서비스 관점의 종합 점검 포함)
- [x] Step C: Generate `tech-stack-decisions.md`

## Question (GATE)

### Question 1: CORS 정책
지금까지 어떤 Unit도 CORS를 설정하지 않았습니다. SocialLogin Unit이 프런트엔드 리다이렉트 URL(`OAUTH2_FRONTEND_REDIRECT_URI`, 로컬 기본값 `http://localhost:3000`)을 이미 전제하고 있어, 프런트엔드가 다른 오리진에서 이 API를 직접 호출할 가능성이 높습니다. SECURITY-08은 "인증된 엔드포인트에 `Access-Control-Allow-Origin: *` 금지"를 요구합니다.

A) 프런트엔드 리다이렉트 URL과 동일한 오리진만 허용 — `app.social-login.frontend-redirect-uri`에서 오리진(scheme+host+port)만 추출해 `Access-Control-Allowed-Origins`로 설정. 로컬 기본값은 `http://localhost:3000`.

B) 별도 환경변수로 CORS 허용 오리진을 명시적으로 관리(리다이렉트 URL과 분리) — 더 유연하지만 설정 항목이 하나 더 늘어남.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## 사전 결정 사항 (질문 없이 적용)

- **401/403 응답 일관성**: 커스텀 `AuthenticationEntryPoint`(401)/`AccessDeniedHandler`(403)를 등록해 다른 Unit과 동일한 `{code, message}` JSON 형식으로 통일한다(Spring Security 기본 응답 대신).
- **PBT 프레임워크**: jqwik 상속.
