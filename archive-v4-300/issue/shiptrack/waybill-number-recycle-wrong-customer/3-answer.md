# issue/shiptrack/waybill-number-recycle-wrong-customer — 정답

태그: `identity` · `data-integrity` · `privacy`

## 정답
<!-- 질문 1:1 대응 -->

1. **같은 번호를 쓰던 2년 전 완료 주문에 붙어 그 주문 고객에게 갔다.**

2. **택배사는 번호 대역을 2년 주기로 재사용한다.**\
계약서 부록에만 적혀 있었다.

3. **사내 연동이 2024년 1월 시작이라 재사용 번호가 처음 돌아왔다.**

4. **매칭 키에 발송일 범위 추가, 완료 30일 지난 주문 매칭 제외, 오발송 알림 고객에게 사과 안내.**\
개인정보(상품명) 노출 여부도 점검했다.

## 문제 구조 (추상화 코드)

### 변형 A — 외부 식별자를 영구 유일로 가정
① 문제 코드
```sql
SELECT order_id FROM shipments WHERE waybill_no = :no;
```
② 고친 코드
```sql
SELECT order_id FROM shipments
WHERE carrier = :carrier AND waybill_no = :no
  AND shipped_at > now() - interval '60 days' AND status <> 'DELIVERED_FINAL';
```
무엇이 깨졌나: 외부가 발급한 번호의 유일성 범위를 확인하지 않고 영구 키로 썼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 키에 기간 범위 추가 | 발송일 기록 | 작음 | 장기 배송 예외 | 기본 |
| 완료 주문 매칭 제외 | 상태 관리 | 작음 | 반품 이벤트 누락 | 기본 |
| 택배사 고유 송장 ID 요청 | 택배사 지원 | 협의 | 미지원 | 장기 |

**결론**: 외부 식별자는 유일성의 범위(기간·발급자)와 함께 쓴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
