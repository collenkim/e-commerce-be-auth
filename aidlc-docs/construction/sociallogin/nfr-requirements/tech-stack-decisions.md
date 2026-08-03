# Tech Stack Decisions — Unit: SocialLogin

| 영역 | 선택 | 근거 |
|---|---|---|
| OAuth2 클라이언트 | `spring-boot-starter-oauth2-client` | Q1:B |
| Provider 등록 | Google=내장 `CommonOAuth2Provider`, Kakao/Naver=커스텀(`authorization-uri`/`token-uri`/`user-info-uri` 수동 설정) | 카카오/네이버는 Spring 내장 목록에 없음 |
| Provider 자격증명 | 환경변수 (`KAKAO_CLIENT_ID/SECRET`, `NAVER_CLIENT_ID/SECRET`, `GOOGLE_CLIENT_ID/SECRET`) | SECURITY-12, 실제 값은 이번 단계 없음(사전 결정) |
| 프런트엔드 콜백 URL | 환경변수 (`OAUTH2_FRONTEND_REDIRECT_URI`) | 로컬 기본값은 Infrastructure Design에서 지정 |
| PBT 프레임워크 | jqwik | Token/Account Unit에서 상속 |

## Shared Infrastructure 갱신 필요 사항

- 없음 — 새 컨테이너/데이터스토어 불필요(MariaDB만 사용, 기존 공유 인스턴스).
