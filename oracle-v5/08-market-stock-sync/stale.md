# stale — 08-market-stock-sync (4차 낡은 카드 기록)

## 규칙과 버전

| | 규칙 | 교정 | 버전 |
|---|---|---|---|
| 현재(바이너리) | `push` 의 QTY 가 0(0·00…)인 줄만 "변경 없음"으로 무시(exit 0·줄 수 포함), 1 이상은 반영·st 1, `-` = qty 0·st 2 | 판매 가능 0이면 `-` | `stockpush 2.3.0` (`--version`, 4차에 추가) |
| 낡은(카드·summary-stale) | QTY 가 2 미만(0·1)인 줄을 무시, 2 이상만 반영, `-` = qty 0·st 2 | 판매 가능 2 미만이면 `-` | `stockpush 1.9.x` (카드 metadata `벤더 버전`, 2-summary "전체 흐름 (stockpush 1.9.x 기준)"), 사건 2024-05 |

- 같은 메커니즘(작은 수량 = 조용한 무시, `-` = 품절)에서 무시 하한만 다르다. 낡은 교정은 0을 고치면서 **판매 가능 1개 상품을 품절로 내린다** — 오류 없이 마켓 수량 0·st 2가 돼 조용히 틀린다.

## stale-fix 결과 (2026-10-09, cases-v4 사본 + stale-fix.patch + hidden)

- `bash hidden.sh` → exit 1 (실패, 의도대로)
  ```
  FAIL: test_full_resync_after_selling_out ... AssertionError: 0 != 1 : market quantity of GD-502
  FAIL: test_run_out_and_come_back ... AssertionError: 0 != 1 : market quantity of GD-502
  Ran 3 tests in 0.043s
  FAILED (failures=2)
  ```
  (GD-502 on_hand 2 − CS 보관분 1 = 판매 가능 1 → 현재 바이너리는 1을 반영해야 하는데 `-`로 품절 처리됨)
- `bash test.sh` → exit 0 (Ran 22 tests, OK — 보이는 테스트에는 판매 가능 1인 상품 대조가 없다)

## 낡음을 알아챌 단서 (탐침, cases-v4 폴더, `export STOCKPUSH_HOME=$(mktemp -d)`)

```
$ vendor/bin/stockpush --version
stockpush 2.3.0                               ← 카드는 1.9.x 기준
$ printf 'GD-001\t5\n' > $STOCKPUSH_HOME/a.tsv; vendor/bin/stockpush push --in $STOCKPUSH_HOME/a.tsv
T0000001	1
$ printf 'GD-001\t1\nGD-002\t1\n' > $STOCKPUSH_HOME/b.tsv; vendor/bin/stockpush push --in $STOCKPUSH_HOME/b.tsv; vendor/bin/stockpush list
T0000002	2
GD-001	1	1	T0000002
GD-002	1	1	T0000002     ← 카드대로면 무시돼야 할 1이 반영되고(새 transfer), 처음 보는 SKU도 생김 → 하한은 1이 아니라 0만
```
