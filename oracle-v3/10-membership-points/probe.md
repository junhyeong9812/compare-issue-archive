# probe — ptsctl 회원번호 앞자리 0 규칙을 실행만으로 알아내는 경로

전제: 케이스 폴더 cwd, `export PTSCTL_HOME=$(mktemp -d)`. 명령 8개.
B가 실제로 밟을 법한 앞 단계(선택): 테스트 실패에서 004512 잔액 0·4512 잔액 2,758(= 690 + 2,068) → 같은 셋업 뒤
`ptsctl hist 4512`를 보면 004512의 결제 참조(E-PAY-1006-0001·0009)가 4512 원장에 들어가 있다 → 아래 탐침.

판단 포인트:
- 2·3·4: `earn --mbr 004512`는 exit 0·적립 번호를 주지만 `bal 004512`는 0, `bal 4512`에 들어가 있다 — 숫자로만 된 회원번호는 앞자리 0을 떼고 적립된다.
- 5·6: help의 `@MBR` 표기(`--mbr @004512`)로 적립하면 `004512` 원장에 그대로 들어간다(hist에 Q2만).
- 7·8: 숫자 아닌 문자가 섞인 번호(`0A12`)는 앞자리 0이 있어도 그대로 적립된다.
- 결론: earn은 숫자로만 된 회원번호를 수로 읽어 앞자리 0을 버린다(다른 회원·주인 없는 계정으로 적립). bal·hist는 받은 글자 그대로 조회한다.
  대처: 적립 호출의 회원번호를 `@` 표기로 넘긴다(`@` + 발급 번호 그대로). 확인은 회원번호별 `bal`/`hist` 조회로 한다.

## 실행 기록 (실제 출력)

```
$ vendor/bin/ptsctl --help                                   # 1
usage: ptsctl <command> [options]

commands:
  earn  --mbr MBR --pt N --ref REF
        prints the entry number
  bal   MBR
  hist  MBR
        entry, ref, points (tab separated)

  MBR   member number; @MBR is also accepted
  REF   [A-Za-z0-9-], 1..32 chars, once per ledger

environment:
  PTSCTL_HOME   ledger directory (required)
[exit 0]
$ vendor/bin/ptsctl earn --mbr 004512 --pt 100 --ref Q1      # 2
P0000001
[exit 0]
$ vendor/bin/ptsctl bal 004512                               # 3
004512 0
[exit 0]
$ vendor/bin/ptsctl bal 4512                                 # 4
4512 100
[exit 0]
$ vendor/bin/ptsctl earn --mbr @004512 --pt 7 --ref Q2       # 5
P0000002
[exit 0]
$ vendor/bin/ptsctl hist 004512                              # 6
P0000002	Q2	7
[exit 0]
$ vendor/bin/ptsctl earn --mbr 0A12 --pt 5 --ref Q3          # 7
P0000003
[exit 0]
$ vendor/bin/ptsctl bal 0A12                                 # 8
0A12 5
[exit 0]
```
