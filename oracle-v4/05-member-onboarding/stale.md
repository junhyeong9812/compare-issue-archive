# stale — 05-member-onboarding (4차 낡은 카드)

## 규칙과 버전
| | 버전 | 규칙 |
|---|---|---|
| 현재(바이너리) | `idchk --version` → `idchk 3.2.0` | batch 호출당 앞 **12줄**만 판정, 13번째 줄부터 조용히 버림(exit 0, 읽은 줄 수 = 전체, result E31 영구) |
| 낡은 카드 | `idchk 2.4.x` (2024-03 사건) | batch 호출당 앞 **20줄**만 판정, 21번째 줄부터 조용히 버림 — 메커니즘·출력·오류 코드는 같고 상한만 다름 |

카드: `archive-v4-stale/issue/idchk/batch-head-cutoff/` (3차 경로명 "twelve"가 현재 상한을 암시해 메인이 낡은 판 경로를 바꿈).

## stale-fix 결과 (`stale-fix.patch` = 3차 fix.patch와 같은 위치, `idchk_batch_lines = 20`)
- hidden: **실패** — 5개 중 4개 실패(`test_exactly_twelve`만 통과). 예: `AssertionError: unexpectedly None : the vendor never checked M2610013` (test_thirteen).
- test.sh: **실패** — 24개 중 4개 실패. `new members are still waiting for the identity check` — M2610003·005·007·008·013·015·017·020 8명 대기(23명 → 20줄 1회 + 3줄 1회, 첫 호출의 13~20번째 줄 버림).

## 틀렸음을 알아챌 단서 (케이스 폴더 cwd, `export IDCHK_HOME=$(mktemp -d)`)
```
$ vendor/bin/idchk --version
idchk 3.2.0                                  # 카드는 2.4.x
$ for i in $(seq -w 1 20); do printf 'S%s\tTester%s\t19900101\t010000002%s\n' $i $i $i; done > $IDCHK_HOME/s.tsv
$ vendor/bin/idchk batch --in $IDCHK_HOME/s.tsv; vendor/bin/idchk job J0000001
J0000001	20
job J0000001
in 20
v1 10
v2 2                                         # v1+v2 = 12 < in 20 — 카드의 20줄 상한이면 20이어야 함
$ vendor/bin/idchk result S12; vendor/bin/idchk result S13
ref S12
job J0000001
v 1
E31                                          # 13번째 줄부터 버림 [exit 3]
```
또한 stale-fix 상태에서 test.sh가 8명 대기로 실패하는 것 자체가 단서다(20줄 분할인데 버림 발생).
