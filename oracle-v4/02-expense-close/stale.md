# 02 낡은 카드 기록 (실험 v4)

## 규칙
- 현재(바이너리 ledgerctl 5.2.0): 메모가 UTF-8 48바이트를 넘으면 post는 exit 0·영수 번호를 주지만 전표를 st 4(보류)로 저장하고 list에서 뺀다.
- 낡은 카드(ledgerctl 4.6.2, 2024-11 사건): 같은 동작인데 상한이 UTF-8 64바이트(한글 21자 기장·22자 보류, ASCII 64자 기장·65자 보류). 대처도 64바이트 문자 경계 자르기.

## 버전
- `vendor/bin/ledgerctl --version` → `ledgerctl 5.2.0` (v4에서 추가, `--help`에 `ledgerctl --version` 한 줄 추가)
- 카드: metadata `벤더 버전 | ledgerctl 4.6.2`, 본문 3-answer 2번 "ledgerctl 4.6.x 기준", summary-stale "ledgerctl(4.6 기준)"

## stale-fix 결과 (`stale-fix.patch` = fix.patch에서 `ledger_memo_bytes` 48 → 64)
- hidden.sh: exit 1 — `Ran 3 tests ... FAILED (failures=2)` (원장에 EX-2609004·006·010 등 누락: "ledger does not hold every settled claim")
- test.sh: exit 1 — `Ran 25 tests ... FAILED (failures=2)` (`7 != 10 : entry count is short`, 9월 3건 누락 그대로)
- 오류 없이 실행된다(49~64바이트 메모가 조용히 보류).

## 틀렸음을 알아챌 단서 (케이스 폴더에서, `export LEDGERCTL_HOME=$(mktemp -d)`)
```
$ vendor/bin/ledgerctl --version
ledgerctl 5.2.0
$ vendor/bin/ledgerctl post --acct 5110 --amt 1000 --date 2026-09-01 --ref P2 --memo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
R00000001
$ vendor/bin/ledgerctl show R00000001 | grep '^st'
st 4
```
- 버전이 카드(4.6.x)보다 높다.
- ASCII 49바이트 메모가 카드 기준(64바이트 이하 기장)과 달리 st 4로 보류된다 → 현재 상한은 64보다 작다(48/49 탐침으로 48 확정).
- stale-fix 뒤에도 test.sh의 3건 누락이 그대로인 것 자체가 단서다.
