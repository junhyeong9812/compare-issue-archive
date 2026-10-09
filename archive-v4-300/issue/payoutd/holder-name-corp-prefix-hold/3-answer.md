# issue/payoutd/holder-name-corp-prefix-hold — 정답

태그: `silent-failure` · `data-normalization`

## 정답
<!-- 질문 1:1 대응 -->

1. **HOLD_NAME으로 보류된다.** 실행 요약은 정상이고 보류 수는 별도 조회(`payoutd list --st HOLD_NAME`)로만 보인다.

2. **개인 예금주명은 표기 변형이 없다.** 법인 접두 표기만 은행 응답이 바뀌었다.

3. **다른 법인 계좌로 돈이 나갈 수 있다.** 정규화는 법인 형태 표기(`주식회사`·`(주)`·`㈜`) 통일까지만 하고 나머지는 완전 일치를 유지한다.

4. **가맹점별 지급 상태를 본다.** 실행 요약이 아니라 지급 건 단위로 상태가 PAID인지 확인한다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
boolean ok = strip(bankName).equals(strip(registered));
```
② 고친 코드
```java
static String corp(String s){ return strip(s).replaceAll("^(주식회사|\\(주\\)|㈜)|(주식회사|\\(주\\)|㈜)$", ""); }
boolean ok = corp(bankName).equals(corp(registered));
```
무엇이 깨졌나: 외부 응답 표기 변경을 안전 검사가 그대로 불일치로 받아, 보류가 집계에서 숨었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 법인 표기 정규화 + HOLD 경보 | 작음 | 새 표기 변형 | 기본 |
| 예금주 대조 끄기 | 없음 | 오지급 | 선택하지 않음 |

**결론**: 안전 검사는 유지하되 표기 정규화 범위를 좁게 정하고, 보류는 실패처럼 보이게 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
