# 요구사항 확인 질문 (Requirements Verification Questions)

아래 질문에 답변해 주세요. 각 질문의 `[Answer]:` 태그 뒤에 선택한 알파벳을 입력하면 됩니다. 제시된 옵션이 원하는 답변과 맞지 않으면 마지막 옵션(Other)을 선택하고 설명을 적어주세요.

---

## Question 1 — 서비스 범위 (Service Scope)
이 인증/인가 서비스(e-commerce-be-auth)는 이커머스 전체 시스템에서 어떤 역할을 담당하나요?

A) 독립적인 인증 서버 — 로그인/회원가입/토큰 발급을 전담하고, 다른 마이크로서비스들은 발급된 JWT를 자체 검증(stateless)만 함

B) 인증 서버이면서 동시에 매 요청마다 인가(권한) 검증까지 게이트웨이처럼 중계함

C) 아직 다른 서비스가 없고, 이 서비스 하나로 모놀리식 이커머스 백엔드의 인증/인가를 전부 처리함

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 2 — 사용자 유형/권한 모델 (User Roles & Authorization Model)
어떤 사용자 역할과 인가(권한) 모델이 필요한가요?

A) 단순 역할 기반 — USER / ADMIN 두 가지 역할

B) 이커머스형 역할 — USER(구매자) / SELLER(판매자) / ADMIN, 역할별 접근 제어(RBAC)

C) 세분화된 권한 기반 — 역할 + 개별 권한(permission) 조합 (예: ORDER_READ, PRODUCT_WRITE 등)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 3 — 로그인 방식 (Login Methods)
기본 로그인 방식은 무엇인가요? (이메일/비밀번호 로그인이 기본이라고 가정해도 될까요?)

A) 이메일 + 비밀번호 로그인만 지원 (소셜/SSO는 이번 단계에서 제외)

B) 이메일 + 비밀번호 로그인 + 소셜 로그인(OAuth2)을 함께 이번 단계에서 구현

C) 이메일 + 비밀번호 로그인만 우선 구현하고, 소셜 로그인은 향후 확장을 고려한 구조만 반영(실제 프로바이더 연동은 이번 단계 제외)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 4 — 소셜 로그인 프로바이더 (Social Login Providers)
Question 3에서 소셜 로그인을 포함한다고 답한 경우, 어떤 프로바이더가 필요한가요? (해당 없으면 A 선택)

A) 해당 없음 (소셜 로그인 이번 단계 제외)

B) 국내형 — 카카오, 네이버, (선택적으로 구글)

C) 글로벌형 — 구글, 페이스북, 애플

D) 국내 + 글로벌 모두 (카카오, 네이버, 구글, 애플 등)

X) Other (please describe after [Answer]: tag below)

[Answer]: D

---

## Question 5 — JWT 토큰 전략 (JWT Token Strategy)
JWT 토큰 발급 전략은 어떻게 할까요?

A) Access Token만 발급 (짧은 만료 시간, 예: 15~30분), Refresh Token 없음

B) Access Token(단기) + Refresh Token(장기) 조합, Refresh Token은 DB/Redis에 저장하여 재사용 탐지 및 회전(rotation) 지원

C) Access Token(단기) + Refresh Token(장기), Refresh Token은 Stateless(서버 저장 없이 서명 검증만)로 처리

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 6 — 토큰 무효화/로그아웃 (Token Invalidation & Logout)
로그아웃 시 또는 보안사고 발생 시 토큰을 즉시 무효화할 수 있어야 하나요?

A) 예 — 로그아웃 시 Refresh Token을 즉시 폐기해야 하고, Access Token도 블랙리스트(예: Redis)로 즉시 무효화 가능해야 함

B) 예 — Refresh Token 폐기는 필요하지만, Access Token은 짧은 만료 시간에 의존하고 별도 블랙리스트는 불필요

C) 아니오 — 토큰 만료 시간만으로 충분함 (즉시 무효화 기능 불필요)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 7 — Rate Limit 적용 범위 (Rate Limiting Scope)
Rate limit을 어떤 범위에 적용해야 하나요?

A) 로그인/회원가입 등 인증 관련 엔드포인트에만 적용 (무차별 대입 공격 방지 목적)

B) 이 서비스의 모든 API 엔드포인트에 공통 적용

C) 엔드포인트별로 서로 다른 제한치를 설정 (예: 로그인은 엄격하게, 조회성 API는 느슨하게)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 8 — Rate Limit 기준 및 정책 (Rate Limit Threshold & Policy)
IP당 허용 요청 수/시간 정책에 대한 선호가 있나요?

A) 특별한 기준 없음 — 합리적인 기본값으로 설계해도 됨 (예: 로그인 IP당 분당 5회, 일반 API는 IP당 분당 60회 등)

B) 구체적인 기준을 직접 지정하겠음 (Other에 상세 기재)

C) 사용자(계정) 단위 제한도 IP 제한과 함께 필요함 (예: 동일 계정에 대한 로그인 시도 제한도 별도로)

X) Other (please describe after [Answer]: tag below)

[Answer]: C

---

## Question 9 — 동일 IP 차단(Block) 정책 (IP Block Policy)
"동일한 IP 요청에 대한 block 처리"가 의미하는 것을 더 구체화하고 싶습니다.

A) Rate limit 초과 시 일정 시간(예: 몇 분~몇십 분) 동안 해당 IP를 자동으로 차단(HTTP 429 또는 403), 시간 경과 후 자동 해제

B) 로그인 실패가 반복되는 IP(무차별 대입 공격 의심)를 자동 차단, 시간 경과 후 자동 해제

C) A와 B를 모두 적용 (일반 rate limit 초과 차단 + 로그인 실패 반복 차단), 자동 해제

D) 자동 차단 외에 관리자가 수동으로 IP를 영구 차단/해제할 수 있는 기능도 필요함

X) Other (please describe after [Answer]: tag below)

[Answer]: C

---

## Question 10 — Rate Limit/차단 상태 저장소 (Storage for Rate Limit & Block State)
이 서비스는 향후 여러 인스턴스(다중 서버)로 확장될 가능성이 있나요? (Rate limit/차단 상태를 어디에 저장할지 결정에 필요)

A) 예, 다중 인스턴스 확장을 고려해야 함 — Redis 등 외부 저장소 기반의 분산 Rate Limit/차단 처리 필요

B) 아니오, 당분간 단일 인스턴스로 충분함 — 애플리케이션 메모리 기반의 Rate Limit/차단으로 충분함 (추후 Redis로 교체 가능한 구조면 좋음)

C) 잘 모르겠음 — 안전하게 Redis 기반으로 설계해 주세요

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 11 — 비밀번호 정책 및 계정 관리 (Password Policy & Account Management)
회원가입/비밀번호 관련하여 어떤 기능이 필요한가요?

A) 기본 기능만 — 회원가입, 로그인, 비밀번호 암호화 저장(BCrypt 등)

B) 기본 기능 + 이메일 인증(회원가입 시 이메일 검증)

C) 기본 기능 + 비밀번호 재설정(이메일 기반) + 이메일 인증까지 포함

X) Other (please describe after [Answer]: tag below)

[Answer]: C

---

## Question 12 — 데이터베이스 (Database)
build.gradle.kts에 MariaDB 드라이버가 이미 포함되어 있습니다. 이 프로젝트의 데이터베이스로 MariaDB를 계속 사용하면 될까요?

A) 예, MariaDB 사용 (로컬 개발은 Docker Compose 등으로 구성)

B) 아니오, 다른 DB로 변경하고 싶음 (Other에 명시)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question: Security 확장
이 프로젝트에 Security 확장 규칙(SECURITY rules)을 적용할까요?

A) 예 — 모든 SECURITY 규칙을 블로킹(차단) 제약조건으로 적용 (프로덕션급 애플리케이션에 권장)

B) 아니오 — 모든 SECURITY 규칙을 건너뜀 (PoC, 프로토타입, 실험적 프로젝트에 적합)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question: Resiliency 확장
이 프로젝트에 Resiliency Baseline(회복탄력성 기준)을 적용할까요?

**이 확장이 하는 일**: 활성화하면 **AWS Well-Architected Framework (Reliability Pillar)** 및 복원력 검토 가이드에서 파생된 **방향성 있는, 설계 단계의 모범 사례**들을 적용합니다. 요구사항/설계/코드가 장애 허용성(fault tolerance), 고가용성, 관측 가능성(observability), 복구 가능성 쪽으로 향하도록 유도하며, 비즈니스 목표·변경 관리·관측성·고가용성·재해복구·지속적 개선 등 15개 영역을 다룹니다.

**이 확장이 아닌 것**: 이를 활성화한다고 해서 워크로드가 프로덕션에 바로 준비되는 것도 아니고, 가용성/RTO/RPO 목표를 보장하거나 인증해주는 것도 아닙니다. 좋은 복원력 의사결정을 초기에 잡아주는 **출발점**일 뿐이며, 정식 **AWS Well-Architected Review**를 대체하지 않습니다.

결과물은 검증·보강해야 할 **복원력 방향성 초안** 정도로 취급해 주세요 — 완성되고 인증된 결과물이 아닙니다.

A) 예 — Resiliency Baseline을 방향성 있는 모범 사례/설계 가이드로 적용 (비즈니스 크리티컬 워크로드에 권장, 출시 전 검증·보강할 수 있는 출발점으로 활용)

B) 아니오 — Resiliency Baseline을 건너뜀 (PoC, 프로토타입 등 신뢰성보다 빠른 반복이 중요한 프로젝트에 적합)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question: Property-Based Testing 확장
이 프로젝트에 Property-Based Testing(PBT) 규칙을 적용할까요?

A) 예 — 모든 PBT 규칙을 블로킹 제약조건으로 적용 (비즈니스 로직, 데이터 변환, 직렬화, 상태 관리 컴포넌트가 있는 프로젝트에 권장)

B) 부분 적용 — 순수 함수(pure function)와 직렬화 라운드트립(serialization round-trip)에 대해서만 PBT 규칙 적용 (알고리즘 복잡도가 제한적인 프로젝트에 적합)

C) 아니오 — 모든 PBT 규칙을 건너뜀 (단순 CRUD 애플리케이션, UI 전용 프로젝트, 비즈니스 로직이 거의 없는 얇은 통합 레이어에 적합)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---
