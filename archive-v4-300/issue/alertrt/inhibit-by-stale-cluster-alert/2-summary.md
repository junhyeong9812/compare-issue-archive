# issue/alertrt/inhibit-by-stale-cluster-alert — 정리 (힌트)

태그: `observability` · `silent-failure`

## 전체 흐름

```
ClusterDown{env=prod, cluster=test-old}  (라벨 오기, 3주 지속)
inhibit: source ClusterDown → target 전체, equal [env]
ApprovalErrorRate{env=prod, cluster=pay-main} → 억제 → 호출 없음
```

## 핵심 문장

- 억제는 `equal` 라벨이 같은 경보를 **조용히** 막는다 — 막힌 경보는 UI에서만 보인다.
- 폐기한 클러스터의 경보가 라벨 오기로 `env=prod`를 달고 계속 떠 있었다.
- 억제 범위는 `cluster`까지 같아야 맞다.
- 오래 켜진 소스 경보와 억제된 경보 수를 대시보드로 본다.
