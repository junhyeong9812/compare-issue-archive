# probe — ledgerctl 메모 길이 규칙을 실행만으로 알아내는 경로

전제: 케이스 폴더 cwd, `export LEDGERCTL_HOME=$(mktemp -d)`. 명령 8개.
B가 실제로 밟을 법한 앞 단계(선택): 테스트 실패에서 빠진 3건을 확인 → 같은 셋업으로 정산한 뒤 `ledgerctl show <영수번호>`를 보면
빠진 전표만 `st 4`, 나머지는 `st 1` → 빠진 3건의 적요가 모두 길다(바이트 기준 61~68)는 공통점 → 아래 경계 탐침.

판단 포인트:
- 2·3: ASCII 48자는 조회되고 49자는 조회되지 않는다(둘 다 exit 0·영수 번호 발급) → 길이 경계 48.
- 4·5: 한글 16자(48바이트)는 조회되고 17자(51바이트)는 안 된다 — 17자는 글자 수로는 48보다 한참 짧다 → 글자가 아니라 UTF-8 바이트.
- 7·8: 조회에서 빠진 전표는 `show`에서 `st 4`(조회되는 전표는 `st 1`) — 오류 없이 다른 상태로 저장된다.
- 결론: 메모가 UTF-8 48바이트를 넘으면 post는 성공(exit 0, 영수 번호)하지만 전표가 st 4로 남고 list에서 빠진다.
  대처: 원장으로 보내기 전 메모를 48바이트 이하로, 문자 경계를 지켜 자른다(잘못 자른 바이트열은 E17로 거부).

## 실행 기록 (실제 출력)

```
$ vendor/bin/ledgerctl --help
usage: ledgerctl <command> [options]

commands:
  post  --acct CODE --amt N --date YYYY-MM-DD --ref REF --memo TEXT
        prints the receipt number
  list  --from YYYY-MM-DD --to YYYY-MM-DD
        receipt, date, acct, amt, ref, memo (tab separated)
  show  RECEIPT

environment:
  LEDGERCTL_HOME   journal directory (required)
[exit 0]
$ vendor/bin/ledgerctl post --acct 5110 --amt 1000 --date 2026-09-01 --ref P1 --memo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
R00000001
[exit 0]
$ vendor/bin/ledgerctl post --acct 5110 --amt 1000 --date 2026-09-01 --ref P2 --memo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
R00000002
[exit 0]
$ vendor/bin/ledgerctl post --acct 5110 --amt 1000 --date 2026-09-01 --ref P3 --memo 가나다라마바사아자차카타파하가나
R00000003
[exit 0]
$ vendor/bin/ledgerctl post --acct 5110 --amt 1000 --date 2026-09-01 --ref P4 --memo 가나다라마바사아자차카타파하가나다
R00000004
[exit 0]
$ vendor/bin/ledgerctl list --from 2026-09-01 --to 2026-09-30
R00000001	2026-09-01	5110	1000	P1	aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
R00000003	2026-09-01	5110	1000	P3	가나다라마바사아자차카타파하가나
[exit 0]
$ vendor/bin/ledgerctl show R00000002
receipt R00000002
date 2026-09-01
acct 5110
amt 1000
ref P2
st 4
memo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
[exit 0]
$ vendor/bin/ledgerctl show R00000004
receipt R00000004
date 2026-09-01
acct 5110
amt 1000
ref P4
st 4
memo 가나다라마바사아자차카타파하가나다
[exit 0]
```
