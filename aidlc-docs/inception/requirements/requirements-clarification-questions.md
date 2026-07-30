# 요구사항 명확화 질문 (Clarification Questions)

답변을 검토하다가 아키텍처 결정에 영향을 주는 애매한 부분 3가지를 발견했습니다. 아래 질문에 답변해 주세요.

---

## Ambiguity 1: 서비스 범위 — "게이트웨이처럼 중계" (Question 1: B)
Question 1에서 "인증 서버이면서 동시에 매 요청마다 인가 검증까지 게이트웨이처럼 중계함"을 선택하셨습니다. 이는 두 가지 다른 아키텍처를 의미할 수 있어 명확히 하고 싶습니다.

### Clarification Question 1
이 서비스가 다른 마이크로서비스로 가는 모든 API 요청을 실제로 가로채서 전달(프록시)하는 API 게이트웨이 역할까지 하나요?

A) 아니오 — 이 서비스는 게이트웨이가 아님. 다른 마이크로서비스가 자체적으로 이 서비스(또는 공유 라이브러리/공개키)를 통해 "이 토큰이 유효한가? 이 사용자가 이 권한을 가졌는가?"를 검증하는 인증/인가 전용 API(예: `/api/v1/auth/verify`, `/api/v1/auth/authorize`)만 제공하면 됨

B) 예 — 이 서비스가 실제 API 게이트웨이 역할을 하며, 클라이언트의 모든 요청이 이 서비스를 거쳐 인증/인가 확인 후 백엔드 마이크로서비스로 프록시(전달)되어야 함

C) 현재는 다른 마이크로서비스가 없으므로, 우선 A 방식(검증 API 제공)으로 만들되 향후 게이트웨이 연동이 쉽도록 설계만 고려

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Ambiguity 2: 소셜 로그인 프로바이더 목록 (Question 4: D)
Question 4에서 "국내 + 글로벌 모두 (카카오, 네이버, 구글, 애플 등)"을 선택하셨습니다. "등(etc)"이 포함되어 정확한 프로바이더 목록을 확정하고 싶습니다.

### Clarification Question 2
이번 단계에서 실제로 연동할 소셜 로그인 프로바이더를 모두 선택해 주세요. (범위가 넓을수록 구현량이 늘어납니다)

A) 카카오, 네이버, 구글, 애플 (4개, 페이스북 제외)

B) 카카오, 네이버, 구글, 애플, 페이스북 (5개 모두)

C) 카카오, 네이버, 구글만 우선 구현 (애플/페이스북은 향후 확장)

X) Other (please describe after [Answer]: tag below — 원하시는 프로바이더 목록을 직접 나열해 주세요)

[Answer]: C

---

## Ambiguity 3: Refresh Token 저장소 (Question 5: B)
Question 5에서 "Refresh Token은 DB/Redis에 저장"을 선택하셨습니다. "DB/Redis"가 병기되어 있어 정확한 저장소를 확정하고 싶습니다. (Question 10에서 Redis를 다중 인스턴스 Rate Limit/차단 저장소로 이미 사용하기로 하셨습니다.)

### Clarification Question 3
Refresh Token(및 로그아웃 시 즉시 무효화용 Access Token 블랙리스트, Question 6: A)을 어디에 저장할까요?

A) Redis만 사용 — Refresh Token, Access Token 블랙리스트 모두 Redis에 TTL 기반으로 저장 (빠르고 만료 자동 처리, 단 Redis 장애 시 인증 불가)

B) MariaDB만 사용 — Refresh Token을 MariaDB 테이블에 저장 (영속성/감사 추적 용이, TTL은 스케줄러로 정리), Access Token 블랙리스트도 MariaDB 테이블로 관리

C) 하이브리드 — Redis는 Access Token 블랙리스트 + Rate Limit(빠른 조회용), MariaDB는 Refresh Token(감사/영속성 필요)에 사용

X) Other (please describe after [Answer]: tag below)

[Answer]: C

---
