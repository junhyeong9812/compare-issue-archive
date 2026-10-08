# settle — 파트너 일일 정산 배치

파트너 ERP가 매일 밤 떨궈 주는 고정폭 피드(`feed-YYYYMMDD.dat`)와 거래처 마스터(`partners.dat`)를 읽어,
정산 데스크용 텍스트 리포트와 회계 시스템용 CSV를 만든다.

## 실행

```
bin/settle run 20261005        # stage → normalize → aggregate → publish
bin/settle report 20261005     # 게시된 리포트 출력
bin/settle export 20261005     # 게시된 CSV 출력
bin/settle partner P10003      # 마스터 항목 조회
bin/settle check 20261005      # 트레일러 대조(건수·합계) 재검사
```

스케줄러(cron)는 매일 06:10에 `bin/settle run $(date -d yesterday +%Y%m%d)` 를 돌리고, exit 0이 아니면 당직에게 알린다.

## 구성

| 경로 | 내용 |
|------|------|
| `bin/settle` | 진입점. 서브커맨드 분기, 단계 순차 실행 |
| `steps/NN-*.sh` | 단계 스크립트(번호 순 실행). 각자 `_env.sh`로 lib·설정을 읽는다 |
| `lib/` | 공용 함수 (피드 해석, 마스터, 캐시, 금액, 리포트, CSV 등) |
| `conf/settle.conf` | 디렉토리·인코딩·리포트 설정. `SETTLE_*` 환경변수로 덮어쓴다 |
| `conf/layout.conf` | 피드 레코드 바이트 레이아웃(ERP-IF-07 rev.3) |
| `fixtures/` | 샘플 피드·마스터 (운영 데이터에서 금액·이름을 바꾼 것) |
| `tests/` | `bash tests/run.sh` — 단위 테스트 + 샘플 일자 E2E |

## 산출물

- `$OUT_DIR/report-YYYYMMDD.txt` — 거래처별 건수·금액, 비활성 거래처 제외
- `$OUT_DIR/export-YYYYMMDD.csv` — 레코드 단위, UTF-8, 헤더 `date,seq,code,partner,amount,memo`
- `$WORK_DIR/YYYYMMDD/` — 단계 중간 산출물(`feed.dat`, `normalized.tsv`, `totals.tsv`)

## 피드 규격 메모

- 레코드 64바이트 고정, H(헤더) / D(명세) / T(트레일러).
- 금액은 12자리 0 채움 원 단위. 구버전 ERP는 환불을 끝자리 `-`로 표시한다.
- 거래처명 20바이트, 적요 12바이트, 남는 자리는 공백으로 채운다.
- 트레일러의 건수·합계가 읽은 값과 다르면 normalize 단계에서 중단한다.
