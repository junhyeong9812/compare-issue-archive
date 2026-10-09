# probe — idchk 배치 처리 건수 규칙을 실행만으로 알아내는 경로

전제: 케이스 폴더 cwd, `export IDCHK_HOME=$(mktemp -d)`. 명령 7개(파일 생성은 같은 줄의 루프).
B가 실제로 밟을 법한 앞 단계(선택): 테스트 실패에서 남은 11명을 확인 → 같은 셋업 후 `idchk result <회원ID>`가 남은 회원만 `E31`
→ `idchk job <job>`이 `in 23`인데 `v1 + v2 = 12` → 일부 줄이 판정 없이 사라졌다는 의심 → 아래 탐침.

판단 포인트:
- 2: `batch`는 15줄 모두 읽었다고 답한다(`J… 15`, exit 0).
- 3: 그런데 `job`의 판정 수 합계는 v1 + v2 = 12 < in 15.
- 4: 판정이 있는 것은 파일의 **앞 12줄**(P01~P12)이고 P13~P15는 `E31`(결과 없음) — 오류 없이 버려졌다.
- 5·6: 30줄을 보내도 판정 수 합계는 12, Q12는 결과가 있고 Q13은 E31 → 내용과 무관하게 호출당 앞 12줄까지만.
- 7: 버려진 줄(P13~P15)만 따로 다시 보내면 정상 판정된다 → 줄 자체는 문제가 없고 호출당 건수 상한이다.
- 결론: `idchk batch`는 한 번 호출에 앞 12줄만 판정하고 나머지는 흔적 없이 버린다(exit 0, 읽은 줄 수는 전체로 출력).
  대처: 12줄 이하로 나눠 여러 번 호출한다. 처리 여부는 `result`(E31 여부) 또는 `job`의 v1+v2 = in 으로 확인한다.

## 실행 기록 (실제 출력)

```
$ vendor/bin/idchk --help
usage: idchk <command> [options]

commands:
  batch   --in FILE
          FILE: ref, name, birth (YYYYMMDD), phone (tab separated)
          prints the job id and the number of lines read
  result  REF
  job     JOBID

environment:
  IDCHK_HOME   working directory (required)
[exit 0]
$ for i in $(seq -w 1 15); do printf 'P%s\tTester%s\t19900101\t010000000%s\n' $i $i $i; done > $IDCHK_HOME/a.tsv; vendor/bin/idchk batch --in $IDCHK_HOME/a.tsv
J0000001	15
[exit 0]
$ vendor/bin/idchk job J0000001
job J0000001
in 15
v1 9
v2 3
[exit 0]
$ for i in $(seq -w 1 15); do printf 'P%s ' $i; vendor/bin/idchk result P$i 2>&1 | tail -1; done
P01 v 1
P02 v 1
P03 v 1
P04 v 2
P05 v 2
P06 v 1
P07 v 1
P08 v 1
P09 v 1
P10 v 2
P11 v 1
P12 v 1
P13 E31
P14 E31
P15 E31
[exit 0]
$ for i in $(seq -w 1 30); do printf 'Q%s\tTester%s\t19900101\t010000001%s\n' $i $i $i; done > $IDCHK_HOME/b.tsv; vendor/bin/idchk batch --in $IDCHK_HOME/b.tsv; vendor/bin/idchk job J0000002
J0000002	30
job J0000002
in 30
v1 10
v2 2
[exit 0]
$ vendor/bin/idchk result Q12; vendor/bin/idchk result Q13
ref Q12
job J0000002
v 1
E31
[exit 3]
$ tail -3 $IDCHK_HOME/a.tsv > $IDCHK_HOME/c.tsv; vendor/bin/idchk batch --in $IDCHK_HOME/c.tsv; vendor/bin/idchk job J0000003; vendor/bin/idchk result P13
J0000003	3
job J0000003
in 3
v1 3
v2 0
ref P13
job J0000003
v 1
[exit 0]
```
