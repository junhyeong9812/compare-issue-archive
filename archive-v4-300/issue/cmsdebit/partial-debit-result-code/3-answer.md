# issue/cmsdebit/partial-debit-result-code — 정답

태그: `data-meaning` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **결과는 0000·출금액 12,000, 사내는 30,000 완납이다.** 18,000원이 미수인데 완납으로 보인다.

2. **부분출금 불허 기관이면 잔액 부족은 불능 코드였다.**

3. **잔액 부족은 불능이 되고 재청구로 간다.** 영업 요구(조금이라도 수납)를 위해 허용은 유지했다.

4. **신청액·출금액·결과 코드를 건별로 비교한다.** 코드만 보면 놓친다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
if (r.code().equals("0000")) bill.setPaid(bill.amount());
```
② 고친 코드
```java
if (r.code().equals("0000")) {
  bill.addPayment(r.debitedAmount());
  if (r.debitedAmount() < bill.amount()) rebill.enqueue(bill, bill.amount() - r.debitedAmount());
}
```
무엇이 깨졌나: 결과 코드를 금액의 대리 지표로 썼고, 기관 설정 변경이 그 의미를 바꿨다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 출금액 기준 수납 + 재청구 | 작음 | 없음 | 기본 |
| 부분출금 불허 | 설정 변경 | 수납률 하락 | 선택하지 않음 |

**결론**: 수납액은 결과 금액으로 기록한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
