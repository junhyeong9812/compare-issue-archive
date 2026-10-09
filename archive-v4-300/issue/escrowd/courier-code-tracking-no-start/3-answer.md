# issue/escrowd/courier-code-tracking-no-start — 정답

태그: `silent-failure` · `data-quality`

## 정답
<!-- 질문 1:1 대응 -->

1. **택배사 코드가 틀린 송장은 추적 결과가 없어 배송 완료 이벤트가 안 생겼다.**

2. **배송 전 구매확정 → 판매자에게 먼저 지급되는 위험.** 그래서 상한(송장 등록 +21일)으로만 썼다.

3. **주문의 배송 추적 상태와 마지막 추적 조회 결과를 본다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```java
if (tracking.delivered(order)) timer.start(order, Duration.ofDays(7));
```
② 고친 코드
```java
if (tracking.delivered(order)) timer.start(order, Duration.ofDays(7));
else if (order.invoiceAge().toHours() > 72 && tracking.noResult(order)) seller.requestCheck(order);
deadline.ensure(order, order.invoiceAt().plusDays(21));
```
무엇이 깨졌나: 오지 않는 이벤트를 기다리는 흐름에 상한이 없었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 무결과 감지 + 상한 기한 | 작음 | 없음 | 기본 |
| 택배사 코드 검증만 | 작음 | 다른 추적 실패 | 보조 |

**결론**: 이벤트 대기 흐름에는 무결과 감지와 상한을 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
