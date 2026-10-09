# issue/glsync/rounding-mode-half-even — 정답

태그: `data-meaning` · `rounding`

## 정답
<!-- 질문 1:1 대응 -->

1. **정산 1,235원, ERP 1,234원이다.** HALF_EVEN은 x.5를 짝수 쪽으로 보낸다.

2. **x.5 끝수가 나오는 건 수와 바로 윗자리의 홀짝에 달렸다.** 건당 최대 1원이다.

3. **당장은 맞지만 재계산 구조가 남는다.** 요율 소수 자리·곱셈 순서가 바뀌면 다시 어긋난다. 결과 금액 전송으로 바꿨다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
gl.send(new Line(acct, sales, rate));      // ERP 가 재계산
```
② 고친 코드
```java
long fee = new BigDecimal(sales).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact();
gl.send(new Line(acct, fee));              // 결과 금액만
```
무엇이 깨졌나: 금액 계산 권한이 두 시스템에 나뉘어 있었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 결과 금액 전송 | ERP 인터페이스 변경 | 없음 | 기본 |
| 양쪽 반올림 모드 통일 | 작음 | 다른 계산 차이 재발 | 임시 |

**결론**: 금액은 원천 한 곳에서 확정해 보낸다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
