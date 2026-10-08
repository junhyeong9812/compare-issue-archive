# probe — xferq 파일 이름 규칙을 실행만으로 알아내는 경로

전제: 케이스 폴더 cwd, `export XFERQ_HOME=$(mktemp -d)`, 아무 내용의 `f.csv`. 명령 10개(`--help` 포함).
B가 실제로 밟을 법한 앞 단계(선택): 테스트 실패에서 10/2·10/5만 빠짐 → `payout manifest`로 그날 이름이
`..._20261002_1.csv`·`_2.csv`(분할 파일)인 것과 접수 번호 확인 → `xferq status <번호>`의 끝 칸이 빠진 파일만 `6`,
받은 파일은 `2` → 받은 이름과 못 받은 이름의 차이(분할 번호가 `_`로 붙음)를 아래 탐침으로 좁힌다.

판단 포인트:
- 2·3: `_` 3개(`A_B_C_D.csv`)는 수신함(ls)에 나오고 4개(`A_B_C_D_E.csv`)는 안 나온다 — 둘 다 exit 0·접수 번호 발급.
- 4: `_` 없이 아주 긴 이름은 받는다 → 길이가 아니다.
- 5: 빈 필드(`A__B__C.csv`, `_` 4개)도 안 나온다 → 필드 의미가 아니라 `_` 문자 개수.
- 6: 합의 접두를 지키고 분할 번호만 `-`로 붙인 이름은 받는다 → 대처.
- 8·9·10: 수신함에 없는 파일은 `status`의 마지막 칸이 `6`, 받은 파일은 `2` — 오류 없이 다른 상태로 남는다.
- 결론: 원격 파일 이름에 `_`가 4개 이상이면 send는 성공(exit 0, 접수 번호)하지만 파일은 st 6으로 남고 파트너 수신함(ls·pull)에 나타나지 않는다.
  대처: 이름의 `_`를 3개 이하로 — 합의 접두 `HBP_SETTLE_<파트너>_<날짜>`는 그대로 두고 분할 번호 등 덧붙이는 부분은 `_` 아닌 구분자(`-` 등)로.

## 실행 기록 (실제 출력)

```
$ vendor/bin/xferq --help
usage: xferq <command> [options]

commands:
  send    --to PARTNER --file PATH [--as NAME]
          prints the ticket
  status  TICKET
          ticket, partner, name, bytes, st (tab separated)
  ls      --to PARTNER
          name, bytes, ticket (tab separated)
  pull    --to PARTNER --name NAME
          writes the file to stdout

environment:
  XFERQ_HOME   spool directory (required)
[exit 0]
$ vendor/bin/xferq send --to P001 --file f.csv --as A_B_C_D.csv
T00000001
[exit 0]
$ vendor/bin/xferq send --to P001 --file f.csv --as A_B_C_D_E.csv
T00000002
[exit 0]
$ vendor/bin/xferq send --to P001 --file f.csv --as HBPSETTLEP00120261005ABCDEFGHIJKLMNOPQRSTUVWXYZ.csv
T00000003
[exit 0]
$ vendor/bin/xferq send --to P001 --file f.csv --as A__B__C.csv
T00000004
[exit 0]
$ vendor/bin/xferq send --to P001 --file f.csv --as HBP_SETTLE_P001_20261005-1.csv
T00000005
[exit 0]
$ vendor/bin/xferq ls --to P001
A_B_C_D.csv	32	T00000001
HBPSETTLEP00120261005ABCDEFGHIJKLMNOPQRSTUVWXYZ.csv	32	T00000003
HBP_SETTLE_P001_20261005-1.csv	32	T00000005
[exit 0]
$ vendor/bin/xferq status T00000002
T00000002	P001	A_B_C_D_E.csv	32	6
[exit 0]
$ vendor/bin/xferq status T00000004
T00000004	P001	A__B__C.csv	32	6
[exit 0]
$ vendor/bin/xferq status T00000001
T00000001	P001	A_B_C_D.csv	32	2
[exit 0]
```
