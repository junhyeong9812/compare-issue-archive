# issue/feecalc/preferential-rate-rep-merchant-only — 정답

태그: `data-meaning` · `partial-update`

## 정답
<!-- 질문 1:1 대응 -->

1. **한 곳(대표 가맹점)만 갱신된다.**

2. **사업자번호 하나에 가맹점 하나라 findOne으로 충분했다.**

3. **적용 시작일부터 소급해 차액을 다음 정산에서 돌려줬다.**

4. **요율이 다른 가맹점들의 공통 속성(같은 사업자번호의 대표 여부)을 본다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```java
merchants.findOne(bizNo).ifPresent(m -> m.setRate(grade.rate()));
```
② 고친 코드
```java
var list = merchants.findAllByBizNo(bizNo);
list.forEach(m -> m.setRate(grade.rate()));
report.add(bizNo, list.size());              // 대상 가맹점 수와 갱신 수 대조
```
무엇이 깨졌나: 1:N 관계를 1:1로 다뤘다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 전체 가맹점 갱신 + 건수 대조 | 작음 | 없음 | 기본 |
| 요율을 사업자 단위로 저장 | 큰 모델 변경 | 가맹점별 예외 처리 불가 | 선택하지 않음 |

**결론**: 사업자 단위 결정은 그 사업자의 모든 가맹점에 적용하고 건수로 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
