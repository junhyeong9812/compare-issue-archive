# issue/feecalc/n-plus-one-rate-lookup — 정리 (힌트)

태그: `performance` · `n-plus-one`

## 전체 흐름

```
거래 320만 건 / 가맹점 4만
for txn: rateRepo.find(txn.merchantId, txn.date)   ← 거래마다 쿼리
      │  평소 60만 건 → 연말 5배
      ▼
쿼리 320만 회 × 3ms ≈ 2.7h + 나머지
```

## 핵심 문장

- 요율은 가맹점·기간 단위로 바뀌는데 **거래 단위로 조회**했다.
- 평소 데이터량에서는 40분이라 지나쳤다.
- 교정: 배치 시작 시 가맹점 요율 이력을 한 번에 올려 메모리 맵으로 조회.
