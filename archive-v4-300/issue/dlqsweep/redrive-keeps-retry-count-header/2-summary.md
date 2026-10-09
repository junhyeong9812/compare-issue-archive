# issue/dlqsweep/redrive-keeps-retry-count-header — 정리 (힌트)

태그: `retry` · `silent-failure`

## 전체 흐름

```
원 처리: 소비자 재시도 5회 실패 → DLQ (헤더 x-retry-count=5)
장애 원인 수정 후: dlqsweep redrive --from settle.dlq --to settle
   메시지 + 헤더 그대로 재발행 (x-retry-count=5)
소비자: retry-count >= 5 → 처리하지 않고 DLQ 로 (처리 시도 0회)
```

## 핵심 문장

- 소비자는 `x-retry-count`가 한도 이상이면 **처리를 시도하지 않고** DLQ로 보낸다.
- `dlqsweep`는 헤더를 보존해 재발행한다 — 원인 추적용으로는 좋지만 재시도 횟수까지 이어졌다.
- 최근 버전 소비자만 이 헤더를 확인해, 일부 토픽만 "재처리가 안 되는" 것처럼 보였다.
- 대처: redrive 시 `x-retry-count`를 0으로 초기화하고 원본 값은 `x-original-retry-count`로 보존, 재처리 결과 건수 보고.
