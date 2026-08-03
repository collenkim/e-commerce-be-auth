# Infrastructure Design — Unit: Authorization

새 컨테이너/데이터스토어 불필요. 이 Unit의 모든 컴포넌트는 애플리케이션 내부 로직(필터/핸들러/컨트롤러)이다.

## 설정 값 (초안)

| 항목 | 값 |
|---|---|
| CORS 허용 오리진 | `app.social-login.frontend-redirect-uri`에서 파생(별도 프로퍼티 불필요) |
