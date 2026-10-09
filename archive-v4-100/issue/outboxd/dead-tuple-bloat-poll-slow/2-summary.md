# issue/outboxd/dead-tuple-bloat-poll-slow — 정리 (힌트)

태그: `performance` · `database`

## 전체 흐름

```
outboxd: 발행 후 DELETE (초당 3천 행)
autovacuum 기본 임계(20%) → 큰 테이블에서 vacuum 드묾 + 장기 트랜잭션(분석 쿼리)이 정리 방해
죽은 튜플 수억 개 → 인덱스 스캔이 죽은 항목을 계속 건너뜀 → 폴링 1회 4초
```

## 핵심 문장

- 삭제된 행은 vacuum 전까지 **죽은 튜플**로 남고 인덱스 스캔 비용을 키운다.
- 같은 DB의 장기 분석 트랜잭션이 정리를 막았다.
- 대처: 테이블별 autovacuum 임계 낮춤(1%), 분석 쿼리는 복제본으로, 아웃박스를 시간 파티션으로 나눠 오래된 파티션 통째 삭제.
