# issue/batchd/month-end-full-list-oom — 정리 (힌트)

태그: `performance` · `memory`

## 전체 흐름

```
FeeAggregateStep: List<Txn> all = repo.findByMonth(ym)   ← 5월 거래 3,100만 건
   힙 8GB → GC 과부하 → OOM
batchd 재시도 3회 → 같은 지점에서 실패 → 수동 개입
```

## 핵심 문장

- 월 거래량이 1년 새 2.6배가 됐는데 집계 스텝은 여전히 **전체를 리스트로** 읽었다.
- 재시도는 같은 입력으로 같은 지점에서 실패할 뿐이다 — 자원 부족형 실패에는 의미가 없다.
- 대처: 커서 스트리밍 + 가맹점 단위 부분 합, 결과는 가맹점별로 커밋.
