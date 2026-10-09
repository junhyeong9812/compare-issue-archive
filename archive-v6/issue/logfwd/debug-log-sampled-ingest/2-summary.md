# issue/logfwd/debug-log-sampled-ingest — 결정 요약

태그: `cost` · `logging` · `vendor-contract`

## 결정
로그 수집기는 결제 API의 **DEBUG 로그를 요청 ID 기준 1/20로 샘플링해서만** 로그 SaaS로 보낸다. INFO 이상과 결제 실패 요청(응답 코드 5xx·PG 오류)의 로그는 DEBUG까지 전량 보낸다. 서버 로컬 로그는 전량을 7일 보관한다(LOG-233, 2025-10-06).

## 핵심 이유
로그 SaaS 계약은 **하루 수집량 300GB까지 정액, 초과분은 GB당 단가 3배**이고 초과 일수가 월 3일을 넘으면 다음 분기 약정량이 자동 상향된다 — DEBUG 켜진 배포 사흘로 그 조건에 걸렸다.

## 흐름
```
결제 API 로그 ─▶ logfwd ─┬ INFO 이상 ──────────────▶ SaaS (전량)
                         ├ 실패 요청(5xx·PG 오류) ─▶ SaaS (DEBUG 포함 전량)
                         ├ DEBUG (그 밖) ── hash(요청ID) % 20 == 0 ─▶ SaaS
                         └ 전체 ─────────────────────▶ 서버 로컬 7일
```
