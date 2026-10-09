# issue/nodepool/spot-only-single-zone-outage — 정리 (힌트)

태그: `availability` · `capacity`

## 전체 흐름

```
nodepool: spot-a (영역 a) 비용 최저 → 어드민 6개 전부 배치
영역 a 스팟 회수 → 6개 동시 종료 → 새 노드 확보 12분
```

## 핵심 문장

- 스케줄러는 **분산 제약이 없으면** 비용·자원 기준으로 몰아 넣는다.
- 스팟 회수는 **같은 영역·같은 유형에서 동시에** 일어난다.
- 2분 통지 동안 다른 스팟 그룹도 같은 상황이라 자리가 없었다.
- `topologySpreadConstraints`(영역)와 온디맨드 최소 비율을 둔다.
