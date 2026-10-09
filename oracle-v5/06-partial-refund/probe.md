# probe — pgsim 배치 내 같은 환불 줄 규칙을 실행만으로 알아내는 경로

전제: 케이스 폴더 cwd, `export PGSIM_HOME=$(mktemp -d)`, 배치 파일은 printf로 만든다(바이너리 호출 아님). 바이너리 명령 9개.
B가 실제로 밟을 법한 앞 단계(선택): 테스트 실패에서 C-2051 둘째 배터리 줄만 "환불 처리 중"·PG 환불 15,900 → 같은 셋업 뒤
작업 폴더의 배치 파일(`RB...-1.tsv`)과 `pgsim status --batch RB...-1`을 비교하면 보낸 5줄 중 4줄만 status에 있다 →
빠진 줄은 앞줄과 TID·금액·셋째 칸이 모두 같은 줄 → 아래 탐침.

판단 포인트:
- 3·4·5: 같은 TID·같은 금액·같은 AUX 두 줄을 한 배치로 보내면 `accepted 2`·exit 0인데 status엔 1줄, txn refunded도 1건 금액뿐.
- 6·7: AUX가 없어도(빈 값끼리 같음) 똑같이 1건만 처리된다.
- 8·9: 같은 TID·같은 금액이어도 AUX가 다르면 2건 모두 처리된다.
- (선택) 같은 줄을 배치 번호를 바꿔 따로 보내면 둘 다 처리된다 — 비교 범위는 한 호출(배치) 안이다. 같은 배치 번호를 다시 보내면 아무것도 안 하고 `accepted N`만 낸다.
- 결론: 한 배치 안에서 (TID, 금액, AUX)가 같은 줄은 첫 줄만 처리하고 나머지는 조용히 버린다. 접수 응답은 파일 줄 수 그대로다.
  대처: 환불 줄마다 AUX를 다르게(상품 줄 식별자 등 결정적 값) 넣는다. 처리 확인은 `status`/`txn` 조회로 한다.

## 실행 기록 (실제 출력, 임시 경로는 /tmp/p로 표기)

```
$ vendor/bin/pgsim --help                                          # 1
usage: pgsim <command> [options]

commands:
  pay     --mid MID --amt N --oid OID
          prints the TID
  refund  --mid MID --batch BID FILE
          FILE: one line per refund: TID<TAB>AMOUNT[<TAB>AUX]
          AUX: optional, [A-Za-z0-9-], 1..24 chars
          prints "accepted N"
  status  --batch BID
          refund no, tid, amount, aux (tab separated)
  txn     TID

environment:
  PGSIM_HOME   state directory (required)
[exit 0]
$ vendor/bin/pgsim pay --mid M1 --amt 38700 --oid O1                # 2
T00000001
[exit 0]
$ printf 'T00000001\t15900\tO1\nT00000001\t15900\tO1\n' > /tmp/p/b1.tsv
$ vendor/bin/pgsim refund --mid M1 --batch B1 /tmp/p/b1.tsv         # 3
accepted 2
[exit 0]
$ vendor/bin/pgsim status --batch B1                                # 4
RF00000001	T00000001	15900	O1
[exit 0]
$ vendor/bin/pgsim txn T00000001                                    # 5
tid T00000001
mid M1
oid O1
amt 38700
refunded 15900
bal 22800
[exit 0]
$ printf 'T00000001\t6900\nT00000001\t6900\n' > /tmp/p/b2.tsv
$ vendor/bin/pgsim refund --mid M1 --batch B2 /tmp/p/b2.tsv         # 6
accepted 2
[exit 0]
$ vendor/bin/pgsim status --batch B2                                # 7
RF00000002	T00000001	6900	-
[exit 0]
$ printf 'T00000001\t7000\tx1\nT00000001\t7000\tx2\n' > /tmp/p/b3.tsv
$ vendor/bin/pgsim refund --mid M1 --batch B3 /tmp/p/b3.tsv         # 8
accepted 2
[exit 0]
$ vendor/bin/pgsim status --batch B3                                # 9
RF00000003	T00000001	7000	x1
RF00000004	T00000001	7000	x2
[exit 0]
```
