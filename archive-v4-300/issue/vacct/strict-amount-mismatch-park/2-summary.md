# issue/vacct/strict-amount-mismatch-park — 정리 (힌트)

태그: `silent-failure` · `contract-drift`

## 전체 흐름

```
vacct issue --amount 39,000 --mode STRICT     ← 2024-06 기본값이 STRICT 로 변경
고객 입금 39,500 (배송비 착오) 또는 38,000
      │  STRICT: 금액 다르면 입금 거절 아님 → 입금 수납 + 통지 보류 (st MISMATCH)
      ▼
사내: 통지 없음 → 주문 입금 대기 유지, 고객 문의
```

## 핵심 문장

- `vacct`는 STRICT 모드에서 금액이 다른 입금을 **은행 단에서 받긴 하지만 통지하지 않고** MISMATCH로 보관한다.
- 2024-06 벤더 기본 모드가 `ANY` → `STRICT`로 바뀌었고, 사내는 모드를 명시하지 않았다.
- 정확한 금액을 넣은 고객은 정상이라 "일부만" 늦었다.
- 교정: 모드를 명시(`RANGE`, 허용 오차 설정)하고, MISMATCH 건을 매시 조회해 처리한다.
