# issue/blobgw/dr-region-read-replication-lag — 정리 (힌트)

태그: `stale-read` · `replication`

## 전체 흐름

```
업로드(주 리전) → 통지 → 파트너 GET(보조 리전) → 404
                           └ 교차 리전 복제 (비동기, 수 초~수 분)
2분 뒤 재시도 → 200
```

## 핵심 문장

- 교차 리전 복제는 **비동기**다 — 주 리전 쓰기 성공이 보조 리전 존재를 뜻하지 않는다.
- 파트너 계정은 DR 훈련 때 보조 리전으로 바뀐 뒤 되돌리지 않았다.
- 복제 상태는 객체 메타데이터의 `replication-status`(PENDING→COMPLETED)로 확인한다.
- 통지는 파트너가 읽는 리전에서 객체가 확인된 뒤 보낸다.
