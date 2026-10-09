# issue/cpnengine/burden-report-reissue-double-count — 정답

태그: `data-semantics` · `reconciliation` · `aggregation`

## 정답
<!-- 질문 1:1 대응 -->

1. **3월에 주문 취소 후 쿠폰이 복원돼 다시 쓰인 주문이 있었다.**

2. **사용 이력의 "사용 이벤트" 건수 × 분담금으로 집계했다.**\
취소된 사용도 이벤트로 남아 있었다.

3. **정산은 주문의 최종 할인액에서 분담금을 계산했다.**

4. **리포트 집계 기준을 주문 최종 할인액으로 변경, 두 시스템 월간 대사, 판매자 31곳에 정정 리포트.**

## 문제 구조 (추상화 코드)

### 변형 A — 이벤트 건수로 금액 집계
① 문제 코드
```sql
SELECT seller_id, COUNT(*) * burden FROM coupon_use_events WHERE ym = '2025-03' GROUP BY seller_id;
```
② 고친 코드
```sql
SELECT seller_id, SUM(final_coupon_discount * burden_rate)
FROM orders WHERE settled_ym = '2025-03' AND status <> 'CANCELLED' GROUP BY seller_id;
```
무엇이 깨졌나: 이벤트 로그를 상태 테이블처럼 집계해 취소된 사건까지 셌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 주문 최종값 기준 | 주문 테이블 접근 | 작음 | 주문 테이블 지연 반영 | 기본 |
| 이벤트에 취소 상쇄 이벤트 추가 | 이벤트 설계 변경 | 중간 | 상쇄 누락 | 대안 |
| 월간 두 시스템 대사 | 대사 잡 | 작음 | 대사 결과 방치 | 병행 |

**결론**: 금액 리포트는 사건 수가 아니라 최종 상태에서 집계한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
