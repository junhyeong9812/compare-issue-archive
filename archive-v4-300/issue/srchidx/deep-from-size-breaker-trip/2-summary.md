# issue/srchidx/deep-from-size-breaker-trip — 정리 (힌트)

태그: `performance` · `resource-limit`

## 전체 흐름

```
from=90000,size=1000 → 샤드마다 상위 91000건 정렬 → 코디네이터가 455000건 병합
힙 사용 급증 → GC 정지 → 같은 노드의 일반 검색 지연
```

## 핵심 문장

- from+size는 **샤드마다 from+size 건**을 정렬해 올린다 — 페이지가 깊을수록 비용이 선형으로 는다.
- 코디네이터 노드 힙이 차며 GC가 길어져 **같은 노드의 모든 검색**이 느려진다.
- `max_result_window` 상향은 깊이 제한을 풀 뿐 비용은 그대로다.
- 내보내기는 `search_after` + PIT(시점 고정)로 바꾼다.
