# issue/cashrcpt/cancel-orig-date-today-reject — 정답

태그: `contract-drift` · `async-status`

## 정답
<!-- 질문 1:1 대응 -->

1. **접수는 성공하고 국세청 전송에서 '원거래 없음'으로 반려된다.**

2. **당일 취소는 오늘 날짜 = 원승인일자였다.**

3. **올바른 원승인일자로 새 취소 요청을 보낸다.** 반려 건은 효력이 없어 중복 문제가 없다.

4. **국세청 기준 발급·취소 상태를 승인번호로 조회한다.** 사내 상태만 보면 취소된 것으로 보인다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
receipt.cancel(orig.approvalNo(), LocalDate.now());
```
② 고친 코드
```java
receipt.cancel(orig.approvalNo(), orig.approvedDate());
// 익일: result(approvalNo) == CANCELLED 확인, REJECTED 면 경보
```
무엇이 깨졌나: 원거래 식별 쌍의 한쪽을 현재 값으로 채웠다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 원승인일자 사용 + 결과 확인 | 작음 | 없음 | 기본 |
| 당일만 취소 허용 | 작음 | 환불 업무 불가 | 선택하지 않음 |

**결론**: 원거래 참조는 저장된 원 값으로 하고, 결과로 완료를 판정한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
