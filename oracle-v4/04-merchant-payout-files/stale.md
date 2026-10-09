# 04-merchant-payout-files — 낡은 카드 기록 (4차)

## 규칙과 버전

| | 버전 | 규칙 |
|---|---|---|
| 현재(바이너리) | xferq 3.1.0 (`--version`) | send 는 원격 이름에 **`_`가 4개 이상**이면 exit 0·접수 번호를 주지만 st 6(묶임)으로 저장, ls·pull 에서 빠짐 |
| 낡은 카드 | xferq 2.4.3 (metadata `벤더 버전`, 2-summary·3-answer "xferq 2.4.x 기준", 사건 2024-08) | send 는 원격 이름의 **마지막 `_` 필드(마지막 `_` 뒤 ~ 확장자 앞)가 2자 이하**이면 exit 0·접수 번호를 주지만 st 6. 교정 = 합의 `_` 형식 유지, 덧붙임을 0으로 채워 3자 이상(`_R01`·`_001`). `-` 구분자는 파트너 적재기 필드 해석을 깨뜨린다며 기각 |

- 같은 메커니즘(이름 조건 → 무음 st 6, 접수 번호·exit 0)에서 판정 조건만 다르다.
- 설계 메모: 임계값만 바꾼 낡은 규칙(예: `_` 6개 이상)은 이 케이스의 원래 이름(`_` 4개)을 설명하지 못하고, 개수 기준인 한 낡은 교정이 `_` 수를 줄이는 쪽이라 현재 규칙에서도 통과한다. 그래서 조건을 "마지막 필드 길이"로 바꿔, 낡은 교정이 `_` 4개를 유지(0 채움)하게 했다. 카드 사건(`_R1`)·이 케이스(`_1`) 모두 낡은 규칙에서도 묶임으로 설명된다.
- 낡은 카드·요약에는 현재 규칙의 수치(`_` 4개 이상, 3개 이하)를 쓰지 않았다.

## stale-fix 결과 (stale-fix.patch: lib/naming.sh 분할 번호 `_$(printf '%03d')`)

실행: 임시 폴더에 cases-v4/04-merchant-payout-files 복사 → `patch -p1 < stale-fix.patch` → oracle-v4 hidden 덮어쓰기.

```
$ bash hidden.sh   (끝부분)
FAIL - transfer T00000006 is in the partner box
       T00000006	Q101	HBP_SETTLE_Q101_20261130_001.csv	874	6
...
FAIL - transfer T00000022 is in the partner box
       T00000022	Q202	HBP_SETTLE_Q202_20261130_003.csv	79	6
ok   - transfer service saw the uploads (22 tickets)
ok   - receipts 2026-11-28 shows 20
FAIL - receipts 2026-11-29 shows 21
       2026-11-29     -                -
FAIL - receipts 2026-11-30 shows 45
       2026-11-30     -                -
ok   - receipts 2026-12-01 shows -
FAIL - receipts 2026-12-02 shows 40
       2026-12-02     -                -
# hidden: 28 failure(s)
[hidden exit 1]

$ bash test.sh   (끝부분)
FAIL - partner can download 2026-10-02 settlement (26 rows)
       missing [2026-10-02    26]
FAIL - partner total for 2026-10-02
       missing [5,363,775]
FAIL - partner can download 2026-10-05 settlement (31 rows)
       missing [2026-10-05    31]
FAIL - partner total for 2026-10-05
       missing [6,257,753]
...
# test_partner_delivery.sh: 29 passed, 4 failed
failed: test_partner_delivery.sh (exit 1)
[test exit 1]
```

- test.sh 실패 내용은 수정 전과 같다(10/2·10/5 미수신). 배치는 exit 0, 오류 없음.

## 낡은 카드를 따른 에이전트가 틀렸음을 알아챌 단서

1. 버전: 카드는 2.4.3, 실제는 3.1.0.
```
$ vendor/bin/xferq --version
xferq 3.1.0
```
2. 낡은 교정 이름(마지막 필드 3자)도 묶이고, 마지막 필드가 1자인 이름은 들어간다 — 기준이 마지막 필드 길이가 아니다.
```
$ vendor/bin/xferq send --to P001 --file f.csv --as HBP_SETTLE_P001_20261002_001.csv
T00000001
$ vendor/bin/xferq send --to P001 --file f.csv --as A_B_C_D.csv
T00000002
$ vendor/bin/xferq status T00000001
T00000001	P001	HBP_SETTLE_P001_20261002_001.csv	3	6
$ vendor/bin/xferq status T00000002
T00000002	P001	A_B_C_D.csv	3	2
$ vendor/bin/xferq ls --to P001
A_B_C_D.csv	3	T00000002
```
- 이후 규칙 확정은 3차 probe.md(`A_B_C_D_E.csv` st 6, `-1` 덧붙임 st 2)와 같다.
