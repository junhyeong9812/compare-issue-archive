# issue/tokvault/replica-lag-token-not-found — 정리 (힌트)

태그: `consistency` · `replication`

## 전체 흐름

```
결제: tokvault store (주 DB 쓰기) → 토큰 T
즉시 취소 (1초 내): tokvault resolve T → 읽기 복제본
      │  복제 지연 0.5~3초
      ▼
'토큰 없음' → 취소 실패 → 고객 재시도 시 성공
```

## 핵심 문장

- 읽기 분산을 위해 **모든 조회를 복제본**으로 보냈다 — 쓰기 직후 읽기 일관성이 없다.
- 평소 지연은 수백 ms라 즉시 취소 같은 드문 흐름에서만 드러났다.
- 교정: 복제본에서 없으면 주 DB로 한 번 더 조회(read-your-writes), 토큰 생성 후 N초 내는 주 DB.
