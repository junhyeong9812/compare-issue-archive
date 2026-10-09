# probe — stockpush 수량 0 규칙과 품절 표기를 실행만으로 알아내는 경로

전제: 케이스 폴더 cwd, `export STOCKPUSH_HOME=$(mktemp -d)`. 명령 8개(파일 생성은 같은 줄의 printf).
B가 실제로 밟을 법한 앞 단계(선택): 테스트 실패에서 품절 3개가 마켓에 예전 수량으로 남은 것을 확인 → 같은 셋업 후 `stockpush show <sku>`를 보면
품절 상품만 `transfer`가 첫 동기화 번호에 머물러 있다(두 번째 push가 건드리지 않음) → 두 번째 push에서 그 줄은 `SKU\t0` → 아래 탐침.

판단 포인트:
- 1: 도움말은 줄 형식이 `SKU<TAB>QTY`, QTY는 "digits or -"라고만 한다 — `-`의 의미는 말하지 않는다.
- 2·3: 0을 보낸 줄도 줄 수에 포함되고(`T… 3`) exit 0이다.
- 4·5: 그런데 0을 보낸 GD-002는 예전 수량 3, st 1, 예전 transfer 그대로이고, 처음 보는 GD-004는 아예 생기지 않았다(E31) → 0은 "변경 없음"으로 조용히 무시된다.
- 6: `-`를 보내면 qty 0, st 2로 바뀐다(처음 보는 SKU도 qty 0·st 2로 생긴다) → `-`가 0으로 내리는 유일한 표기.
- 7: 음수(-1)는 E15로 거부 — 다른 우회 표기는 없다.
- 8: `-` 이후 양수를 보내면 qty 4, st 1로 정상 복귀 → `-`는 되돌릴 수 있는 품절 표시.
- 결론: `stockpush push`는 QTY가 0인 줄을 성공 응답 그대로 무시한다. 수량을 0으로 만들려면 QTY 자리에 `-`를 보내야 한다.
  대처: 피드 줄을 만들 때 판매 가능 수량이 0 이하면 `-`를 쓴다.

## 실행 기록 (실제 출력)

```
$ vendor/bin/stockpush --help
usage: stockpush <command> [options]

commands:
  push  --in FILE
        FILE: one item per line, SKU<TAB>QTY   (QTY: digits or "-")
        prints the transfer id and the number of lines
  list
        sku, qty, st, transfer (tab separated)
  show  SKU

environment:
  STOCKPUSH_HOME   store directory (required)
[exit 0]
$ printf 'GD-001\t5\nGD-002\t3\nGD-003\t7\n' > $STOCKPUSH_HOME/a.tsv; vendor/bin/stockpush push --in $STOCKPUSH_HOME/a.tsv
T0000001	3
[exit 0]
$ printf 'GD-001\t2\nGD-002\t0\nGD-004\t0\n' > $STOCKPUSH_HOME/b.tsv; vendor/bin/stockpush push --in $STOCKPUSH_HOME/b.tsv
T0000002	3
[exit 0]
$ vendor/bin/stockpush list
GD-001	2	1	T0000002
GD-002	3	1	T0000001
GD-003	7	1	T0000001
[exit 0]
$ vendor/bin/stockpush show GD-002; vendor/bin/stockpush show GD-004
sku GD-002
qty 3
st 1
transfer T0000001
E31
[exit 3]
$ printf 'GD-003\t-\nGD-004\t-\n' > $STOCKPUSH_HOME/c.tsv; vendor/bin/stockpush push --in $STOCKPUSH_HOME/c.tsv; vendor/bin/stockpush list
T0000003	2
GD-001	2	1	T0000002
GD-002	3	1	T0000001
GD-003	0	2	T0000003
GD-004	0	2	T0000003
[exit 0]
$ printf 'GD-002\t-1\n' > $STOCKPUSH_HOME/d.tsv; vendor/bin/stockpush push --in $STOCKPUSH_HOME/d.tsv
E15
[exit 3]
$ printf 'GD-003\t4\n' > $STOCKPUSH_HOME/e.tsv; vendor/bin/stockpush push --in $STOCKPUSH_HOME/e.tsv; vendor/bin/stockpush show GD-003
T0000004	1
sku GD-003
qty 4
st 1
transfer T0000004
[exit 0]
```
