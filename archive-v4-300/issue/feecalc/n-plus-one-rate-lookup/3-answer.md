# issue/feecalc/n-plus-one-rate-lookup — 정답

태그: `performance` · `n-plus-one`

## 정답
<!-- 질문 1:1 대응 -->

1. **거래 수가 5배로 늘어 쿼리 수가 같이 늘었다.**

2. **배치는 시작 시점 스냅숏을 쓴다.** 배치 중 요율 변경은 다음 정산에 반영하는 규칙을 명시했다.

3. **배치별 쿼리 수 ÷ 처리 건수 지표를 넣었다.** 1에 가까우면 N+1이다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
for (Txn t : txns) fee += t.amount() * rateRepo.find(t.merchantId(), t.date()).rate();
```
② 고친 코드
```java
RateBook book = rateRepo.loadAll(period);            // 한 번
for (Txn t : txns) fee += t.amount() * book.rate(t.merchantId(), t.date());
```
무엇이 깨졌나: 데이터량에 비례하는 원격 호출.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 요율 일괄 로딩 | 작음 | 메모리 | 기본 |
| DB 인덱스 튜닝 | 작음 | 호출 수 그대로 | 보조 |

**결론**: 배치는 참조 데이터를 한 번에 올린다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
