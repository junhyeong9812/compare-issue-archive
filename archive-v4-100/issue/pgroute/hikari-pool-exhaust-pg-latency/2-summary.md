# issue/pgroute/hikari-pool-exhaust-pg-latency — 정리 (힌트)

태그: `performance` · `resource-exhaustion`

## 전체 흐름

```
요청 → @Transactional 시작 (DB 커넥션 획득)
         ├─ 결제 레코드 INSERT
         ├─ PG 호출 (평소 300ms → 행사 때 4s)      ← 커넥션을 쥔 채 대기
         └─ 결과 UPDATE, 커밋
풀 크기 50 / 초당 120건 × 4s = 동시 약 500 필요 → 커넥션 대기 30s → 타임아웃
```

## 핵심 문장

- 외부 호출을 **트랜잭션 안에서** 하면 외부 지연이 그대로 DB 커넥션 점유 시간이 된다.
- PG는 느렸을 뿐 실패하지 않았다 — 실패는 우리 풀 대기에서 났다.
- 교정: 결제 레코드 생성 커밋 → PG 호출(트랜잭션 밖) → 결과 반영 커밋으로 나눈다.
- 풀 크기만 키우면 DB 최대 연결 수에 부딪힌다.
