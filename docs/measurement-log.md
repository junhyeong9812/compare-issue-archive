# measurement-log

| 날짜 | 작업 | stakes | 워커(모델) | 결과 |
|---|---|---|---|---|
| 2026-10-08 | case-build | 낮음 | 4 (Opus) | 케이스 10/10 verify ×3 PASS |
| 2026-10-08 | token-experiment | 낮음 | 0 (claude -p sonnet 60회 — 실험 대상) | A≈B, 60/60 성공, 비용 합 $4.79 |
| 2026-10-08 | token-experiment 추가(F) | 낮음 | 0 (claude -p sonnet 33회) | F÷B 토큰 1.74(중앙값), 30/30 성공, 감사 오탐 3 수정 |
| 2026-10-08 | experiment-v2 | 낮음 | 4 (Opus, 케이스 작성) + claude -p sonnet 100회 | 성공 B28/F28/P30, 토큰 F×1.87·P×1.35 |
| 2026-10-08 | experiment-v2 추가(Haiku) | 낮음 | claude -p haiku 93회 | 성공 B29/P30/F30, P×1.49·F×1.76, 비용 합 $1.19 |
| 2026-10-08 | plite-experiment(arm L, P-lite) | 낮음 | claude -p sonnet 30 + haiku 30회 | 유효 60, 성공 30/30·30/30, L÷B Sonnet 1.12·Haiku 1.05 |
| 2026-10-09 | experiment-v3(ADR-0003) | 낮음 | 워커 Opus 9회(케이스·카드) + claude -p sonnet 128 + haiku 120 | 유효 240, ÷B Sonnet L0.42/P0.49/F0.53 · Haiku L0.55/P0.61/F0.64 |
