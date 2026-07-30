# Resiliency Baseline — 필수 요구사항 질문

Resiliency Baseline 확장을 활성화하셨기 때문에, 아키텍처(DR 전략, 변경 관리, 리전 구성)에 직접 영향을 주는 아래 질문에 답변이 필요합니다.

---

## Question: RTO/RPO 목표 및 재해 복구(DR) 전략
목표 복구 시간(RTO)과 목표 복구 시점(RPO)은 어느 정도인가요? 이 답변이 재해 복구 전략과 인프라 이중화 수준을 결정합니다.

A) RPO/RTO: 시간(Hours) 단위 — Backup & Restore 전략. 최저 비용($). 데이터는 백업만 되어 있고 서비스는 평소 미배포. 장애 시 IaC로 재배포 후 백업 복원. 비핵심 워크로드에 적합

B) RPO/RTO: 수십 분 단위 — Pilot Light 전략. 비용 $$. 데이터는 실시간, 서비스는 대기 상태. 장애 조치 시 스케일업. 중요 워크로드에 적합

C) RPO/RTO: 분 단위 — Warm Standby 전략. 비용 $$$. 데이터는 실시간, 서비스는 축소된 용량으로 상시 운영. 장애 조치 시 스케일업. 비즈니스 크리티컬 애플리케이션에 적합

D) RPO/RTO: 준실시간 — Multi-site Active/Active 전략. 최고 비용($$$$). 여러 리전에서 서비스 동시 운영. 미션 크리티컬, 무중단 요구사항에 적합

E) 해당 없음 — 단일 리전 배포로 충분함. 리전 간 DR 불필요, 단일 리전 내 멀티 AZ 가용성으로 충분

X) Other (please describe after [Answer]: tag below)

[Answer]: E

---

## Question: 변경 관리 프로세스 (Change Management Process)
프로덕션 변경 사항을 어떻게 관리할까요?

A) 기존 조직 변경 관리 프로세스 사용 — 도구/이름 명시 (예: ServiceNow, Jira Change, 내부 CAB) — Other에 기재

B) 정해진 프로세스 없음 — AI-DLC가 경량 변경 관리 프로세스(변경 기록 + 승인 + 롤백 노트)를 제안

C) 해당 없음 — 이 워크로드는 정식 변경 관리에서 예외 (사유는 Other에 기재)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question: 리전 토폴로지 (Regional Topology)
이 서비스는 멀티 리전 배포가 필요한가요, 아니면 단일 리전 + 멀티 AZ로 충분한가요? (위 RTO/RPO 답변과 일관되어야 합니다 — 예: C 또는 D를 선택했다면 보통 멀티 리전이 필요합니다)

A) 단일 리전, 멀티 AZ — Zone 장애는 견디지만 리전 전체 장애는 대응 불가. 저비용 (RTO/RPO A/B/E와 부합)

B) 멀티 리전 Active-Passive — 리전 장애 시 페일오버로 생존. 고비용 (Warm Standby/Pilot Light의 크로스 리전 버전과 부합)

C) 멀티 리전 Active-Active — 리전 장애 시에도 무중단 생존. 최고비용 (Active/Active와 부합)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---
