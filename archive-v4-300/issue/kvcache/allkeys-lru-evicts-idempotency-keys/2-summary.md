# issue/kvcache/allkeys-lru-evicts-idempotency-keys — 정리 (힌트)

태그: `data-loss` · `config`

## 전체 흐름

```
프로모션 캐시 대량 적재 → used_memory = maxmemory
allkeys-lru → 최근에 안 쓴 키부터 축출
   └ payout:* (쓰고 나서 재조회 거의 없음) → 축출 대상 1순위
재시도 들어옴 → 멱등 키 없음 → 다시 지급
kvcache stats: evicted_keys 1.2M/h
```

## 핵심 문장

- `allkeys-lru`는 TTL 유무와 상관없이 **오래 안 읽힌 키**를 지운다 — 멱등 키는 쓰고 나서 거의 안 읽혀 가장 먼저 지워진다.
- 축출은 오류가 아니다 — 앱은 '키 없음'을 '처음 요청'으로 해석한다.
- `volatile-*` 정책도 TTL 있는 키를 지우므로 멱등 키(TTL 24시간)는 마찬가지다.
- 정합성이 걸린 키는 **축출 없는 저장소**(`noeviction` 별도 인스턴스 또는 DB 유니크 제약)에 둔다.
