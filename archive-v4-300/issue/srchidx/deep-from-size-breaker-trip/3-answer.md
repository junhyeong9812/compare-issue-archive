# issue/srchidx/deep-from-size-breaker-trip — 정답

태그: `performance` · `resource-limit`

## 정답
<!-- 질문 1:1 대응 -->

1. **각 91,000건**, 코디네이터는 455,000건을 병합한다.

2. **코디네이터 역할 노드가 공유였기 때문이다.** GC 정지 동안 그 노드를 거치는 모든 요청이 멈췄다.

3. **아니다.** 오히려 더 깊은 페이징을 허용해 피해를 키운다 — 당시 누군가 20만으로 올려 둔 상태였다.

4. **`search_after` + PIT.** 페이지마다 비용이 일정하고, 내보내기 중 색인돼도 결과가 흔들리지 않는다.

## 문제 구조 (추상화 코드)

### 변형 A — 깊은 페이징으로 전체 내보내기
① 문제 코드
```python
for page in range(0, total, 1000):
    rows += es.search(index="txn", from_=page, size=1000, sort="ts")["hits"]["hits"]
```
② 고친 코드
```python
pit = es.open_point_in_time(index="txn", keep_alive="2m")["id"]
after = None
while True:
    hits = es.search(pit={"id": pit, "keep_alive": "2m"}, size=1000,
                     sort=[{"ts": "asc"}, {"_shard_doc": "asc"}], search_after=after)["hits"]["hits"]
    if not hits: break
    rows += hits; after = hits[-1]["sort"]
```
무엇이 깨졌나: 화면용 페이징 API를 대량 추출에 재사용했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| search_after + PIT | 정렬 키 | 작음 | PIT 만료 | 기본 |
| 원장 DB에서 직접 추출 | 복제본 | 중간 | DB 부하 이동 | 규모가 아주 클 때 |
| max_result_window 상향 | 없음 | 작음 | 클러스터 지연 | 선택하지 않은 방법 |

**결론**: 내보내기를 search_after로 바꾸고 `max_result_window`는 기본값으로 되돌린다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
