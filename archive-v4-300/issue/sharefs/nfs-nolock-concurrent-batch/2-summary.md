# issue/sharefs/nfs-nolock-concurrent-batch — 정리 (힌트)

태그: `concurrency` · `duplicate`

## 전체 흐름

```
노드 A: flock(settle.lock) → 성공 (로컬 잠금)
노드 B: flock(settle.lock) → 성공 (로컬 잠금)
둘 다 settle_20231012.dat 생성 → 같은 이름에 다른 순번 접미 → 파트너가 둘 다 수신
```

## 핵심 문장

- `nolock` 마운트에서는 잠금이 **서버로 가지 않고 노드 로컬**에만 걸린다.
- 같은 노드 안의 잠금은 되므로 단일 노드 테스트로는 안 보인다.
- NFS v4는 잠금이 프로토콜에 포함되지만, 장애 시 잠금 회수 지연이 있다.
- 단일 실행은 DB 행 잠금(리더 선출 테이블)이나 스케줄러 수준에서 보장한다.
