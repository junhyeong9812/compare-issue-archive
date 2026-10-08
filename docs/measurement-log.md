# measurement-log

| 날짜 | 작업 | stakes | 워커(모델) | 결과 |
|---|---|---|---|---|
| 2026-10-08 | case-build | 낮음 | 4 (Opus) | 케이스 10/10 verify ×3 PASS |
| 2026-10-08 | token-experiment | 낮음 | 0 (claude -p sonnet 60회 — 실험 대상) | A≈B, 60/60 성공, 비용 합 $4.79 |
| 2026-10-08 | token-experiment 추가(F) | 낮음 | 0 (claude -p sonnet 33회) | F÷B 토큰 1.74(중앙값), 30/30 성공, 감사 오탐 3 수정 |
| 2026-10-08 | experiment-v2 | 낮음 | 4 (Opus, 케이스 작성) + claude -p sonnet 100회 | 성공 B28/F28/P30, 토큰 F×1.87·P×1.35 |
