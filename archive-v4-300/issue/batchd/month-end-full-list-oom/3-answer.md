# issue/batchd/month-end-full-list-oom — 정답

태그: `performance` · `memory`

## 정답
<!-- 질문 1:1 대응 -->

1. **이번 달은 지나가지만 다음 성수기에 다시 넘는다.** 메모리 사용이 거래량에 비례하는 구조가 원인이다.

2. **4월 거래는 2,200만 건으로 힙 안에 들어갔다.** 5월 가정의 달 행사로 40% 늘었다.

3. **가맹점 수에 비례한다**(부분 합 맵). 가맹점 9만 개로 수십 MB 수준이다.

## 문제 구조 (추상화 코드)

### 변형 A — 전체 결과 적재
① 문제 코드
```java
List<Txn> all = repo.findByMonth(ym);
Map<Long, Fee> fees = all.stream().collect(groupingBy(Txn::merchantId, summing()));
```
② 고친 코드
```java
Map<Long, FeeAcc> acc = new HashMap<>();
try (Stream<Txn> s = repo.streamByMonth(ym, 5_000)) {        // fetch size 5000 커서
    s.forEach(t -> acc.computeIfAbsent(t.merchantId(), FeeAcc::new).add(t));
}
acc.values().forEach(feeRepo::upsert);
```
무엇이 깨졌나: 입력 크기에 비례하는 메모리 사용을 고정 힙에 맡겼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 커서 스트리밍 | 드라이버 커서 지원 | 작음 | 긴 트랜잭션 | 기본 |
| 가맹점 구간 분할 실행 | 분할 키 | 중간 | 분할 누락 | 데이터 더 커질 때 |
| 힙 증설 | 노드 여유 | 작음 | 재발 | 임시 조치만 |

**결론**: 집계 배치는 입력이 아니라 결과 크기에 비례하도록 짜고, 자원 부족 실패는 재시도하지 않는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
