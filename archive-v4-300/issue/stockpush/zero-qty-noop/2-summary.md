# issue/stockpush/zero-qty-noop — 정리 (힌트)

태그: `silent-failure` · `contract-drift`

## 전체 흐름

```
피드 ──push --in FILE──▶ stockpush
       SKU<TAB>QTY         │
                           │ QTY = 양수 (1~99999)  → qty = QTY, st 1 (판매 중), transfer 갱신
                           │ QTY = 0 / 00 ...     → 아무것도 안 함 (예전 qty·st·transfer 유지,
                           │                         처음 보는 SKU 는 생기지도 않음)
                           │ QTY = "-"            → qty 0, st 2 (판매 중 아님), transfer 갱신
                           │ QTY = -1, 2.5 ...    → E15 (파일 전체 거부)
                           │
                           └─ 0 줄이 있어도: stdout "T000000x<TAB>N" (N = 전체 줄 수), exit 0

0 을 그대로 보내는 호출부
  판매 가능 0 → "SKU\t0" → 무시 → 마켓은 예전 수량으로 판매 중 → 주문 → 출고 불가 취소
올바른 호출부
  판매 가능 0 → "SKU\t-" → qty 0, st 2
확인: list / show 의 qty·st·transfer (push 응답의 줄 수로는 알 수 없다)
```

## 핵심 문장

- `stockpush push`는 **QTY 0 줄을 "변경 없음"으로 조용히 무시**한다 — exit 0, 줄 수에도 포함된다.
- 수량을 0으로 내리는 표기는 **`-`** 다 — qty 0·st 2가 되고, 이후 양수를 보내면 st 1로 돌아온다.
- 도움말은 `-`를 허용 값으로만 적는다 — "건너뜀"으로 읽기 쉬운데 실제로는 **품절**이다.
- 반영 확인은 push 응답이 아니라 **list/show의 qty·st·transfer**로 한다 — 무시된 줄은 transfer가 갱신되지 않는다.
