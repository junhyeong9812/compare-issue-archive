# issue/sellerrpt/replica-lag-report-tail-missing — 정답

태그: `replication` · `data-freshness` · `partial-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **읽기 복제본이 23:20 근처까지만 반영된 상태였다.**\
행사로 복제 지연이 40분 넘게 쌓였다.

2. **평소 복제 지연은 1초 미만이었다.**

3. **복제가 따라잡은 뒤라 정상으로 나온다.**\
그래서 "다시 돌리니 맞다"로 넘어갈 뻔했다.

4. **생성 전 복제본 지연이 마감 시각을 지났는지 확인, 안 되면 대기·경보, 리포트 머리에 데이터 기준 시각 표시.**

## 문제 구조 (추상화 코드)

### 변형 A — 복제본 신선도 확인 없이 마감 조회
① 문제 코드
```python
rows = replica.query(DAILY_SQL, day=d)
build_report(rows)
```
② 고친 코드
```python
wait_until(lambda: replica.replayed_until() >= end_of(d) + timedelta(minutes=1), timeout=hours(2))
rows = replica.query(DAILY_SQL, day=d)
build_report(rows, as_of=replica.replayed_until())
```
무엇이 깨졌나: 복제본을 주 DB와 같은 시점으로 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 복제 지연 확인 후 생성 | 지연 조회 가능 | 작음 | 대기 시간 | 기본 |
| 주 DB 조회 | 주 DB 여유 | 작음 | 마감 시각 부하 | 대안 |
| 기준 시각 표시 | — | 작음 | 표시만으로는 누락 방지 못함 | 병행 |

**결론**: 마감 리포트는 데이터가 마감 시각을 지났는지 확인한 뒤 만든다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
