# log — token-experiment

## 타임라인

| 시각 | 사건 | 결과/결정 |
|------|------|----------|
| 2026-10-08 | 명세 합의(사용자 "진행하자"), SPEC=1·MODE=auto. 실행 수단 서브에이전트→claude -p 헤드리스로 사용자 제안 반영 | 구독 사용량 차감 안내 |
| 2026-10-08 | 스모크: 격리 HOME claude -p — CLAUDE.md 미인식, usage 정상 | 가정1 일부 실증 |
| 2026-10-08 | bwrap 실패: `setting up uid map: Permission denied` (kernel.apparmor_restrict_unprivileged_userns=1) | 사용자 선택: 권한 거부 규칙 + 로그 감사(시스템 설정 변경 없음). 명세 §2 격리 방식 변경 |
| 2026-10-08 | OAuth 토큰 회전 위험 대응: 실행 전 만료 ≥45분 대기, 실행 후 샌드박스 토큰이 더 새것이면 원본에 원자 되쓰기 | run_experiment.py 반영 |
| 2026-10-08 | 스모크(c) judge: buggy=실패, fix.patch=성공, 테스트 변조 감지 | 통과 |
| 2026-10-08 | 스모크(b) 감사: B 아카이브 경로·웹 study-note 검출, 정상 로그 0건. 격리 HOME deny 규칙이 bypassPermissions에서도 Read 차단("denied by your permission settings") + 감사가 시도 자체를 검출 | 통과(fail-open 아님) |
| 2026-10-08 | 스모크(a) 04 A/B 각 1회: 둘 다 success, A $0.081 / B $0.074, 4턴, A archive_tool_calls=1 | results/smoke-runs.jsonl 로 분리. 본 실험 착수 |
| 2026-10-08 | 본 실험 60회 완료(exit 0): 유효 60·성공 60·무효 0·러너 오류 0·토큰 되쓰기 0 | summarize.py → results/summary.md |
| 2026-10-08 | 손 검산: 10-B 총 토큰 [88753,114767,114863] 중앙값 114,767 = 표 일치 / 05-A 비용 중앙값 0.0797 = 표 0.080 일치 | 집계 정상 |
| 2026-10-08 | 관찰: A의 아카이브 접근 10/30회, 전부 첫 명령의 README head -50 — 카드 열람 0회. 전 실행 도구 3~4회로 해결 | 해석 results/analysis.md. 케이스 난이도가 아카이브 필요 상황을 만들지 못함 — 차기 설계 후보 4개 |

## 완료 요약
- A/B 토큰 차이 없음(중앙값 89.2k vs 88.0k, 비용 합 $2.39 vs $2.40). 원인은 아카이브 미활용 — 케이스가 작아 모델이 코드 전체를 한 번에 읽고 사전 지식으로 해결.
- 산출: scripts/run_experiment.py(격리 HOME claude -p·deny 규칙·로그 감사·판정·토큰 되쓰기 가드), scripts/summarize.py, results/(runs.jsonl·raw 로그·summary·analysis).
- 아카이브(CS 이슈): ① Ubuntu AppArmor `apparmor_restrict_unprivileged_userns=1`이 비특권 bwrap을 막음 ② 격리 HOME에서 실행하는 claude -p의 OAuth 갱신이 원본 토큰을 회전시킬 수 있는 위험 — study-note 카드 후보, 사용자 확인 후 아카이브(이번 범위 밖 → 보류, NEXT 등재).
| 2026-10-08 | 추가 실험 합의(arm F archive-first, 1차 케이스 ×3). 러너에 F 프롬프트·card_tool_calls, summarize에 arm 동적 처리 추가 | 정규식 스모크 통과, 1차 summary 재생성 동일 수치 |
| 2026-10-08 | F 30회 완료: 유효 27·무효 3(03 r1·05 r2·10 r2) — 감사 오탐: Claude Code 권한 가드 거부 메시지가 deny 규칙 문자열을 인용(접근 시도 아님) | audit()에서 가드 메시지 내 규칙 문자열 마스킹 + 실접근은 계속 검출(단위 확인). 무효 3행은 보존, 3건 재실행 |
| 2026-10-08 | F 재실행 3건 유효·성공. 최종 F 30/30 유효·성공, 총 토큰 중앙값 153,163(B 88,020), 비용 합 $3.80(B $2.40) | summary 재생성 |
| 2026-10-08 | 손 검산: F 카드 분류 정답 10·엉뚱 3·없음 17 = 30. README 499줄/65,793B를 F 30회 전부가 열람 | ADR-0001·analysis에 추가 실험 append, ADR-0002 개정 append(arm B/F/A′) |
