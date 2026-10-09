# stale — 06-partial-refund (4차 낡은 카드)

## 규칙과 버전
| | 버전 | 규칙 |
|---|---|---|
| 현재(바이너리) | `pgsim --version` → `pgsim 4.1.0` | 한 refund 배치 안에서 **(TID, 금액, AUX)**가 모두 같은 줄은 첫 줄만 환불, 나머지 조용히 버림(accepted = 파일 줄 수, exit 0). AUX가 다르면 각각 환불 |
| 낡은 카드 | `pgsim 3.3.x` (2024-04 사건) | 한 refund 배치 안에서 **(TID, 금액)**이 같은 줄은 첫 줄만 환불 — AUX는 판정에 쓰이지 않는다(메모 칸). 대처 = 겹치는 줄을 새 배치 번호의 다음 배치로 |

메커니즘(배치 안 비교·accepted = 줄 수·같은 배치 번호 재전송 무시)은 같고 비교 키 조건만 다르다.

## stale-fix 결과 (`stale-fix.patch`)
- 위치: `src/service/RefundBatchJob.java` — 청크 안에서 같은 (TID, 금액) 줄을 다음 배치로 분리(배치 번호 = 회차-순번).
  3차 fix.patch(`RefundFileWriter.merchantRef`)와 **위치가 다르다**: 낡은 규칙에서는 AUX가 판정에 쓰이지 않으므로 AUX를 바꾸는 낡은 수정은 성립하지 않는다(AUX를 줄마다 다르게 하면 어떤 낡은 조건으로도 현재 규칙을 통과해 버려 hidden이 실패하지 않는다).
- hidden: **실패** — 4개 중 3개 실패.
  `FAIL - November returns: PG pays every returned item: RB20261103-01 PG calls (expected 1, got 3)`,
  `FAIL - small chunks: ...: RB20261103-01 PG calls (expected 3, got 6)`,
  `FAIL - sample week: ...: RB20261008-01 PG calls (expected 1, got 2)`. 환불 금액·RF 번호 자체는 맞고, 회차 배치 수 = 청크 수 계약(README "청크당 PG 호출 1번")이 깨진다.
- test.sh: **실패** — unit 6/6 통과, e2e 7/8 통과. `FAIL - nightly refund run submits every requested item: PG batches (expected 1, got 2)`.

## 틀렸음을 알아챌 단서 (케이스 폴더 cwd, `export PGSIM_HOME=$(mktemp -d)`)
```
$ vendor/bin/pgsim --version
pgsim 4.1.0                                  # 카드는 3.3.x
$ vendor/bin/pgsim pay --mid M1 --amt 38700 --oid O1
T00000001
$ printf 'T00000001\t15900\tO1-1\nT00000001\t15900\tO1-2\n' > $PGSIM_HOME/b.tsv
$ vendor/bin/pgsim refund --mid M1 --batch B1 $PGSIM_HOME/b.tsv; vendor/bin/pgsim status --batch B1
accepted 2
RF00000001	T00000001	15900	O1-1
RF00000002	T00000001	15900	O1-2           # AUX만 다른 같은 TID·금액 두 줄이 둘 다 환불 — 카드와 반대
$ vendor/bin/pgsim txn T00000001 | grep refunded
refunded 31800
```
또한 stale-fix 상태에서 test.sh가 "PG batches expected 1, got 2"로 실패하는 것이 배치 분리 대처가 이 케이스의 계약과 맞지 않는다는 단서다.
