# issue/billkey/batch-threads-pg-ip-throttle — 정리 (힌트)

태그: `performance` · `rate-limit`

## 전체 흐름

```
배치 시간 단축 목적: 스레드 8 → 64
PG 한도: 출발 IP 당 초당 100건
64 스레드 × 초당 3건 ≈ 192 → 초과분 429/E-THROTTLE
      ▼
billkey: E-THROTTLE 을 '결제 실패'로 분류 → 회원에게 실패 안내 메일
```

## 핵심 문장

- PG 거절 사유 E-THROTTLE은 **카드 문제가 아니다** — 회원에게 실패 안내를 보내면 안 된다.
- 스레드 증설은 성능 시험 없이 설정값으로만 바꿨다.
- 교정: 초당 90건 토큰 버킷, 스로틀 응답은 재큐잉, 회원 안내는 카드 사유에만.
