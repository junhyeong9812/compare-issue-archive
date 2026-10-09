# issue/chgback/sign-inverted-chargeback-file — 정답

태그: `data-meaning` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **환수 대신 50,000원 지급으로 반영됐다.**

2. **B의 규칙(양수+구분 코드)으로 만든 처리라 B는 맞았다.**

3. **방향이 뒤집혔다는 것이다.** 누락이면 한 배, 부호 반전이면 두 배 차이가 난다.

4. **카드사가 파일 꼬리에 준 합계(부호 포함)와 가져온 금액 합계를 대조.**

## 문제 구조 (추상화 코드)

① 문제 코드
```java
long amt = rec.amount();  // B 기준: 양수 = 환수
ledger.recover(merchant, amt);
```
② 고친 코드
```java
long recover = profile(card).direction() == SIGNED ? -rec.amount()
             : (rec.flag() == 'D' ? rec.amount() : -rec.amount());
ledger.recover(merchant, recover);
check(sum(imported) == trailer.signedTotal());
```
무엇이 깨졌나: 상대마다 다른 금액 방향 표기를 하나로 가정했다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 카드사별 방향 규칙 + 꼬리 합계 대조 | 작음 | 새 카드사 규칙 누락 → 대조 실패 | 기본 |
| 모든 카드사에 표기 통일 요청 | 협의 | 거절 | 선택하지 않음 |

**결론**: 금액의 방향은 상대별 계약으로 해석하고, 부호 포함 합계로 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
