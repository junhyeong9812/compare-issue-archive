# stale — 10-membership-points (ptsctl)

## 규칙

| | 버전 | 앞 0을 버리는 회원번호 | 대처 |
|---|---|---|---|
| 현재(바이너리) | ptsctl 4.2.0 (`--version`) | 숫자로만 된 번호 **전부**(길이 무관: 0000417→417, 010→10, 000000→0) | 모든 earn에 `@` 표기(또는 0으로 시작하는 번호만 `@`) |
| 낡은 카드 | ptsctl 3.6.x (사건 2024-08) | 숫자 **6자리** 번호만(매장 카드 형식). 6자리가 아닌 숫자 번호는 글자 그대로 | 숫자 6자리 번호만 `@` 표기, 그 밖은 그대로 |

같은 메커니즘(earn이 숫자 회원번호를 수로 읽음, `@`만 글자 그대로, bal/hist는 글자 그대로)에서 조건(대상 자릿수)만 다르다.

## stale-fix.patch 결과

`src/infra/PtsctlClient.java` earn에 `mbr()` — `[0-9]{6}`이면 `"@" + 번호`, 아니면 그대로(3차 fix.patch와 같은 위치, 조건만 낡은 규칙).
- `bash test.sh` → **통과** (exit 0, `unit: 5 passed, 0 failed` / `e2e: 9 passed, 0 failed` — 보이는 매장 카드 회원 004512·000873·031207이 모두 6자리).
- `bash hidden.sh` → **실패** (exit 1, `hidden: 2 passed, 1 failed`):
  `FAIL - November day: every member's vendor balance is their own points: balances (expected [], got [0000417 vendor=0 shown=0 want=1191, 417 vendor=1311 shown=1311 want=120, 010 vendor=0 shown=0 want=3000])`
- 조용히 틀린다 — 보이는 테스트가 통과하므로 탐침 없이는 놓친다.

## 틀렸음을 알아챌 단서 (탐침, cwd = 케이스, `export PTSCTL_HOME=$(mktemp -d)`)

```
$ vendor/bin/ptsctl --version                                  # 1 — 카드는 3.6.x, 지금은 4.2.0
ptsctl 4.2.0
[exit 0]
$ vendor/bin/ptsctl earn --mbr 0000417 --pt 100 --ref Q1        # 2 — 7자리(카드라면 그대로 적립)
P0000001
[exit 0]
$ vendor/bin/ptsctl bal 0000417                                #   → 본인 0
0000417 0
[exit 0]
$ vendor/bin/ptsctl bal 417                                    #   → 417 에 쌓임 (카드와 모순)
417 100
[exit 0]
$ vendor/bin/ptsctl earn --mbr 010 --pt 5 --ref Q2              # 3 — 3자리도 마찬가지
P0000002
[exit 0]
$ vendor/bin/ptsctl bal 10
10 5
[exit 0]
```
