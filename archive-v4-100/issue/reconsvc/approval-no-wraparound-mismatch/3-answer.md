# issue/reconsvc/approval-no-wraparound-mismatch — 정답

태그: `data-meaning` · `identity`

## 정답
<!-- 질문 1:1 대응 -->

1. **둘이 짝지어져 금액 불일치로 나오고, 올해 4월의 진짜 짝은 미대사로 남는다.**

2. **미대사 잔류분 정리가 지연돼 1년 넘은 건이 대사 대상에 남았다.**

3. **카드사 매입일과 승인일이 하루 어긋날 수 있어 ±1일 범위로 맞췄다.**

4. **금액 불일치 건의 날짜 차이가 수백 일이었다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```sql
JOIN card_txn c ON c.approval_no = o.approval_no
```
② 고친 코드
```sql
JOIN card_txn c ON c.card_co = o.card_co AND c.approval_no = o.approval_no
 AND c.auth_date BETWEEN o.auth_date - INTERVAL 1 DAY AND o.auth_date + INTERVAL 1 DAY
```
무엇이 깨졌나: 유일성 범위가 제한된 외부 식별자를 전역 키로 썼다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 복합 키 + 날짜 범위 | 작음 | 없음 | 기본 |
| 잔류분 정리만 | 작음 | 재발 | 보조 |

**결론**: 외부 식별자의 유일성 범위를 키에 함께 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
