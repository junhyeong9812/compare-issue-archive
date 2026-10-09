# payout — 가맹 파트너 일일 정산 파일 배치

카드 매입 추출(`txns-YYYYMM.tsv`)에서 파트너별·일자별 정산 내역을 잘라 정산 파일(PAY-IF-03 rev.2)을 만들고,
파일 전송 벤더 CLI(`vendor/bin/xferq`, 벤더가 바이너리로 제공)로 파트너 수신함에 올린다.

## 실행

```
bin/payout run 20261005                    # extract → build → ship
bin/payout manifest 20261005               # 그날 전송 CLI에 넘긴 파일과 접수 번호
bin/payout receipts P001 20261001 20261007 # 파트너가 실제로 내려받을 수 있는 일자별 건수·금액
bin/payout partners                        # 활성 파트너
```

스케줄러는 매일 07:00에 전날 일자로 `bin/payout run`을 돌리고, exit 0이 아니면 당직에게 알린다.
`XFERQ_HOME`(전송 CLI 스풀 디렉터리)은 스케줄러 환경에서 지정한다. 벤더 사용법은 `vendor/bin/xferq --help`.

## 구성

| 경로 | 내용 |
|------|------|
| `bin/payout` | 진입점. 서브커맨드 분기, 단계 순차 실행 |
| `steps/NN-*.sh` | 단계 스크립트(번호 순). 각자 `_env.sh`로 lib·설정을 읽는다 |
| `lib/` | 공용 함수 (추출, 분할, 파일 본문, 파일 이름, 전송, 매니페스트, 수신 확인 등) |
| `conf/payout.conf` | 디렉터리·파일 규격 설정. `PAYOUT_*` 환경변수로 덮어쓴다 |
| `conf/partners.conf` | 파트너 목록(상태 I는 정지 — 파일을 만들지 않음) |
| `fixtures/` | 샘플 매입 추출(운영 데이터에서 금액·번호를 바꾼 것) |
| `tests/` | `bash tests/run.sh` — 단위 테스트 + 샘플 주간 E2E |
| `docs/` | 파트너 인터페이스 발췌, 당직 런북 |
| `CHANGELOG.md` | 변경 이력 |

## 정산 파일 규격 메모 (PAY-IF-03 rev.2)

- 파일명: `HBP_SETTLE_<파트너코드>_<YYYYMMDD>.csv` — 파트너와 합의한 접두. 파트너 쪽 적재기는 이 접두로 파일을 고른다.
- 파트너 적재기는 파일당 명세(D) 최대 20행까지만 받는다. 넘는 날은 여러 파일로 나눠 보낸다(헤더의 `n/총수`).
- 본문: `H`(헤더) / `D`(명세, 거래당 1행) / `T`(트레일러 — 명세 건수·순액 합계). 줄 끝은 CRLF.
- 정산은 주말 포함 매일 나간다. 거래가 없는 날도 빈 파일(H·T만)을 보낸다.
