# Unit of Work — Story Map

모든 스토리(`stories.md`, 총 18개: US-101~104, US-201~203, US-301~304, US-401~405, US-501, US-502, US-603)가 Unit에 할당되었는지 검증 완료.

| Unit | 스토리 (주 소유) | 비고 |
|---|---|---|
| **Token** | US-301, US-302, US-303, US-304 | |
| **Account** | US-101, US-102, US-103, US-104 | US-103(로그인)은 Account가 자격증명 검증을 주 소유하되, Token(토큰 발급 — US-301 참조)과 RateLimit(시도 카운팅 — US-402/US-404 참조)에 의존하는 크로스-유닛 오케스트레이션 스토리 |
| **SocialLogin** | US-201, US-202, US-203 | 각각 Account(계정 조회/생성), Token(토큰 발급)에 의존 |
| **RateLimit** | US-401, US-402, US-403, US-404, US-405 | |
| **Authorization** | US-501, US-502, US-603 | Token(역할 클레임 조회)에 의존 |

## 커버리지 검증

- Epic 1 (회원가입/계정 관리): US-101~104 → Account ✓
- Epic 2 (소셜 로그인): US-201~203 → SocialLogin ✓
- Epic 3 (토큰 관리): US-301~304 → Token ✓
- Epic 4 (Rate Limit/IP 차단): US-401~405 → RateLimit ✓
- Epic 5 (RBAC/인가, US-603 포함): US-501, US-502, US-603 → Authorization ✓

미할당 스토리 없음. (참고: US-601, US-602, US-604는 게이트웨이 범위 제거에 따라 `stories.md`에서 이미 삭제되어 이 매핑에 포함되지 않음.)
