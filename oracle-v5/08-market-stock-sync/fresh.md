# fresh — 08-market-stock-sync (5차-a 맞는 카드 기록)

- 카드: `archive-v5-fresh/issue/stockpush/zero-qty-noop/`
- 현재 규칙·버전: `stockpush 2.3.x` — `push` 의 QTY 가 0(0·00…)인 줄만 "변경 없음"으로 무시(exit 0·줄 수 포함), 양수는 반영·st 1, `-` = qty 0·st 2. 교정 = 판매 가능 0이면 `-`.
- 만든 방법: 3차 카드(archive-v3) 4파일을 그대로 두고, 4차 낡은 카드에서 규칙이 아닌 줄(사건 날짜 2024-05·벤더 버전 행·"전체 흐름 (… 기준)" 머리)만 가져와 버전을 2.3.x로 바꿨다.

## 낡은 카드(archive-v4-stale) 대비 바꾼 줄 (diff 줄 수: `<` 28, `>` 26, 총 54)

| 파일 | 줄 | 내용 |
|---|---|---|
| 1-question.md | 5 | 경계 예시에서 "1" 제거(3차 원문) |
| 1-question.md | 7 | "2 이상의 수량" → "양수" |
| 2-summary.md | 5 | `전체 흐름 (stockpush 1.9.x 기준)` → `(stockpush 2.3.x 기준)` (버전) |
| 2-summary.md | 10~14 | 흐름도 2 이상 / 0·00·1 → 양수(1~99999) / 0·00 |
| 2-summary.md | 16 | "무시된 줄이 있어도" → "0 줄이 있어도"(3차 원문) |
| 2-summary.md | 21 | 판매 가능 2 미만(0·1) → 0 |
| 2-summary.md | 27~28 | 요점 2 미만·2 이상 → 0·양수 |
| 3-answer.md | 14 | "0"·"00"·"1" 무시 → "0"과 "00" 무시 |
| 3-answer.md | 22 | 2 이상의 수량 → 양수 |
| 3-answer.md | 28~30 | 대처 2 미만이면 `-`(+1개 남은 상품 줄) → 0 이하면 `-`(3차 원문, 1줄 감소) |
| 3-answer.md | 43 | 주석 "벤더의 품절 표기" → "벤더의 0 표기"(3차 원문) |
| 3-answer.md | 47, 49, 53, 61 | 변형 A/B 설명 2 미만·0·1 → 0 |
| 3-answer.md | 57 | `MIN_QTY = 2` 줄 삭제(3차에 없음) |
| 3-answer.md | 59 | `qty >= MIN_QTY` → `qty > 0` |
| 3-answer.md | 82~83, 86 | 대처 비교표·결론 2 미만 → 0 (3차 원문) |
| metadata.md | 8 | 벤더 버전 stockpush 1.9.x → stockpush 2.3.x |

## 3차 카드와 규칙 의미가 같다는 근거

- `diff -r archive-v3/… archive-v5-fresh/…` 차이는 날짜 2곳(3-answer 87행 2024-05-14, metadata 초안 2024-05-14 — 4차 낡은 카드 값 유지)과 버전 표기 2곳(2-summary 5행 머리, metadata 벤더 버전 행)·metadata 사건 행 "(2024-05)"뿐이다. 규칙·대처·예시·코드 줄은 3차와 바이트 단위로 같다.
- 요약 `summary-fresh.txt` 는 3차 `summary.txt` 와 "stockpush(2.3 기준)" 한 곳만 다르다.
- 확인: cases-v4 사본 + fix.patch(3차와 cmp 동일) + hidden → `Ran 3 tests … OK`, exit 0. fix.patch 의 `qty if qty > 0 else SOLD_OUT("-")` 가 카드 대처(`qty > 0 ? qty : SOLD_OUT`, 0 이하면 `-`)와 일치. `--version` = `stockpush 2.3.0`.
