# issue/pgroute/currency-minor-unit-jpy — 정답

태그: `data-meaning` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **120,000 JPY로 전달된다.** 상수 100을 곱했기 때문이다.

2. **두 통화 모두 소수 2자리라 ×100이 맞는 변환이었다.**

3. **10분의 1 금액이 된다.** 3자리 통화에 ×100을 하면 실제보다 작게 승인된다 — 이 경우 고객 불만은 없고 손실만 생겨 더 늦게 발견된다.

4. **통화별 건당 비율(우리 금액 ÷ PG 금액)을 본다.** 비율이 10의 거듭제곱으로 일정하면 단위 변환 문제다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
long minor = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
```
② 고친 코드
```java
int exp = Currency.getInstance(code).getDefaultFractionDigits();   // JPY 0, KWD 3
long minor = amount.movePointRight(exp).longValueExact();
```
무엇이 깨졌나: 통화 단위 변환을 상수로 고정해, 자리수가 다른 통화를 잘못 보냈다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 통화 표 기반 변환 | 작음 | 표에 없는 통화 — 예외로 거부 | 기본 |
| 통화별 하드코딩 분기 | 작음 | 새 통화 추가 시 누락 | 선택하지 않음 |

**결론**: 자리수는 통화 표에서 읽고, 시험 데이터에 0·3자리 통화를 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
