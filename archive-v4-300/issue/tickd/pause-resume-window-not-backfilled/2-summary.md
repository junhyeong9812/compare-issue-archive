# issue/tickd/pause-resume-window-not-backfilled — 정리 (힌트)

태그: `silent-failure` · `ops-procedure`

## 전체 흐름

```
job refund-retry (10분 주기, 입력 = "직전 10분에 실패한 환불")
01:00 pause (DB 점검) ... 03:00 resume
tickd: 정지 중 회차 12개 → 버림 (misfire=DISCARD 기본, paused 작업에 적용)
03:10 회차: 03:00~03:10 실패분만 조회
→ 점검 직전·직후 실패한 환불 214건이 재시도 대상에서 영구 제외
```

## 핵심 문장

- `tickd`는 일시정지된 작업의 회차를 **버린다** — 재개 후 몰아 실행하지 않는다.
- 작업 입력이 "직전 10분"이라는 회차 기준 구간이어서, 버려진 회차의 입력은 다시 조회되지 않았다.
- 점검 절차서에는 "작업 정지 → 점검 → 재개"만 있었다.
- 대처: 입력을 "재시도 상태가 PENDING인 모든 건"으로 바꾸고, 점검 절차에 재개 후 PENDING 0 확인 추가.
