# issue/replwatch/replica-apply-paused-by-report-query — 정리 (힌트)

태그: `stale-read` · `config`

## 전체 흐름

```
주 DB  ── UPDATE account ── VACUUM 으로 옛 행 정리 ──WAL──▶ 복제본
                                                         │ 리포트 쿼리가 옛 행 스냅숏 사용 중
                                                         │ max_standby_streaming_delay = -1
                                                         ▼
                                              WAL 적용 대기 (쿼리 끝날 때까지)
replwatch lag: replay_lag 0.2s → 37m
어드민 조회(복제본) → 옛 계좌
```

## 핵심 문장

- 복제본에서 긴 쿼리가 쓰는 행을 주 DB가 정리하면 **충돌**이 나고, 복제본은 쿼리를 취소하거나 **WAL 적용을 미룬다**.
- `max_standby_streaming_delay = -1`은 무한히 미룬다 — 리포트는 성공하지만 그동안 **모든 테이블** 반영이 멈춘다.
- `replwatch lag`의 `replay_lag`가 수신 지연(`write_lag`)과 따로 크게 벌어지면 이 경우다.
- 리포트는 전용 복제본으로 분리하고, 쓰기 직후 조회는 주 DB에서 읽는다.
