# issue/sellerrpt/fx-rounding-per-line-vs-total — 정답

태그: `rounding` · `reconciliation` · `data-semantics`

## 정답
<!-- 질문 1:1 대응 -->

1. **반올림 위치 차이를 의심한다.**\
주문 수에 비례해 차이가 커지는 것이 단서다.

2. **리포트는 줄마다 원 단위로 반올림한 뒤 더했고, 정산은 USD 합계를 환산해 한 번 반올림했다.**

3. **국내 판매는 환산이 없어 반올림이 필요 없었다.**

4. **리포트 합계를 정산과 같은 계산 순서로, 줄별 원화는 "참고"로 표기, 반올림 규칙을 정책 문서 한 곳에 고정.**

## 문제 구조 (추상화 코드)

### 변형 A — 줄별 반올림 합계
① 문제 코드
```java
long total = lines.stream().mapToLong(l -> round(l.usd().multiply(rate))).sum();
```
② 고친 코드
```java
BigDecimal usdSum = lines.stream().map(Line::usd).reduce(BigDecimal.ZERO, BigDecimal::add);
long total = round(usdSum.multiply(rate));        // 정산과 같은 순서
```
무엇이 깨졌나: 같은 금액을 두 계산 순서로 만들어 반올림 오차가 대사 차이로 보였다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 합계 후 반올림 통일 | 정산 규칙 확인 | 작음 | 줄별 합과 불일치 표시 필요 | 기본 |
| 줄별 오차 배분 | 배분 로직 | 중간 | 복잡 | 대안 |
| 차이 허용 범위 공지 | — | 작음 | 판매자 신뢰 | 선택하지 않은 방법 |

**결론**: 금액 계산은 정산 원천과 같은 순서로 반올림한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
