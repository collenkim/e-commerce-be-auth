# Business Rules — Unit: Token

## 만료 시간 (Q1:A)

| 토큰 | TTL |
|---|---|
| Access Token | 15분 |
| Refresh Token | 14일 |

## 서명 (Q2:A)

- 알고리즘: HMAC-SHA256 (HS256), 대칭키
- 비밀키는 이 서비스가 소유. 외부 검증자와 공유할 경우(로컬 검증 옵션) 안전한 채널로 배포 — 구체적 키 배포/로테이션 방식은 Infrastructure Design 단계에서 다룬다.

## 검증 규칙 (모든 보호된 요청에 공통 적용)

1. Access Token 서명이 유효해야 한다. 유효하지 않으면 즉시 401.
2. Access Token이 만료되지 않아야 한다 (`exp` 확인). 만료 시 401.
3. Access Token의 `jti`가 블랙리스트(Redis)에 없어야 한다. 있으면 401.
4. 위 3가지를 모두 통과해야 유효한 요청으로 간주한다.
5. 검증 API(`validate()`) 호출 자체는 별도 인증 없이 열려 있다 — 내부망 전제 (Q4:A). 공인망에 노출되지 않도록 하는 것은 Infrastructure Design 책임.

## 회전(Rotation) 규칙

1. `ACTIVE` 상태의 Refresh Token만 정상 회전 가능하다.
2. 회전 시 기존 토큰은 즉시 `ROTATED`로 표시하고, 같은 요청 트랜잭션 안에서 새 토큰을 발급한다 (동시성 이슈로 같은 토큰이 두 번 정상 회전되는 것을 막기 위해 DB 트랜잭션/락으로 원자성 보장 — 구현은 Code Generation에서 결정).
3. 회전된 토큰으로 다시 회전을 시도하면 재사용 탐지 규칙(아래)이 적용된다.

## 재사용 탐지 규칙

1. `ACTIVE`가 아닌 상태(`ROTATED` 또는 `REVOKED`)의 Refresh Token으로 갱신을 시도하면 재사용으로 간주한다.
2. 재사용 탐지 시 해당 `familyId`의 모든 Refresh Token 레코드를 `REVOKED`로 전환한다.
3. 재사용 탐지는 사용자에게 별도 알림을 보내지 않는다 (Q3:A) — 감사 로그에만 기록한다 (SECURITY-03/14, 민감정보 제외).
4. **알려진 제약**: 재사용 탐지가 Refresh Token 패밀리는 즉시 막지만, 그 패밀리로 이미 발급되어 아직 만료 전인 Access Token은 자체 만료(최대 15분)까지 유효할 수 있다.

## 로그아웃/무효화 규칙

1. 로그아웃은 Refresh Token을 `REVOKED`로 전환하고, 요청에 포함된 Access Token의 `jti`를 블랙리스트에 등록한다.
2. 비밀번호 변경 시(Account Unit 트리거) 해당 계정의 `ACTIVE` Refresh Token을 전부 `REVOKED`로 전환한다 (패밀리 무관, 계정 전체).
3. 블랙리스트 TTL은 Access Token의 남은 만료 시간과 동일하게 설정한다 (그 이후엔 자체 만료로 어차피 거부되므로).

## 데이터 보관 규칙 (Q5:A)

- `RefreshToken` 레코드는 상태와 무관하게 삭제하지 않는다 (영구 보관, 감사 목적).
- 이 Unit의 범위에서 별도 삭제/아카이빙 배치는 설계하지 않는다.

## 클록 스큐(Clock Skew) 허용 오차

- 별도 언급 없어 기본값으로 0초(허용 오차 없음) 적용. 운영 중 분산 환경에서 문제가 확인되면 Infrastructure Design/Code Generation 단계에서 조정 가능한 설정값으로 둔다.
