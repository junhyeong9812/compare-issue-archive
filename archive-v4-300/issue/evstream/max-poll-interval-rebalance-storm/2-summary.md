# issue/evstream/max-poll-interval-rebalance-storm — 정리 (힌트)

태그: `performance` · `rebalance`

## 전체 흐름

```
max.poll.records=500, max.poll.interval.ms=300000(5분)
모델 서버 지연 0.2s → 0.8s : 500건 × 0.8s = 400s > 300s
→ 소비자가 그룹에서 제외 → 리밸런싱 → 커밋 실패 → 같은 500건 재처리 → 반복
```

## 핵심 문장

- 폴링 사이 처리 시간이 `max.poll.interval.ms`를 넘으면 소비자는 **죽은 것으로 간주**된다.
- 처리 중이던 배치의 커밋이 실패해 같은 배치를 처음부터 다시 처리하는 고리에 빠졌다.
- 대처: `max.poll.records`를 100으로 줄이고, 모델 호출에 타임아웃·병렬 처리.
