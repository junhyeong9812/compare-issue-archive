# 한빛 멤버십 포인트 적립 배치 (membership-points)

한빛페이 제휴 가맹점 결제에 대해 멤버십 포인트를 적립하는 야간 배치와 회원 포인트 조회 백엔드다.
매일 밤 매입사 결제 내보내기(`payments-YYYY-MM-DD.csv`)를 읽어, 승인된 결제마다 회원 등급 적립률로 포인트를 계산해
포인트 벤더에 적립하고, 회원 앱의 포인트 화면은 벤더 잔액을 보여 준다.

## 구성
```
src/api       PointsApi(퍼사드 — 스케줄러·회원 앱 백엔드가 호출), ReportFormatter
src/service   EarnBatchJob, EarnPolicy, MemberDirectory, PaymentDeduper, StatementService, TopEarnersReport
src/infra     PtsctlClient(포인트 벤더 CLI 호출), CsvFiles(회원·결제 파일)
src/store     EarnLedger(적립 전송 기록, 결제당 1회)
src/domain    레코드 타입
src/util      Fmt
conf/         app.properties (POINTS_* 환경변수 / -Dpoints.* 로 덮어쓰기)
fixtures/     회원 목록, 10/06 매입사 결제 내보내기
vendor/bin/   포인트 벤더 제공 CLI `ptsctl` (벤더 배포본 — 저장소에서 수정하지 않는다. `ptsctl --help`)
```

## 규칙
- 회원번호는 발급된 그대로의 문자열이다. 매장 카드 회원은 카드에 찍힌 6자리 번호, 앱 회원은 앱 발급 번호, 법인은 C코드.
- 적립률: BASIC 1%, GOLD 1.5%, VIP 2% (포인트 미만 버림), 결제 1건당 최대 5,000P. 취소 결제는 적립하지 않는다.
- 매입사는 타임아웃 뒤 같은 결제 행을 다시 보내므로 결제 ID 기준으로 한 번만 센다. 벤더 참조는 `E-<결제ID>`.
- 회원 앱의 포인트 잔액·적립 건수는 벤더 조회 값이다. 마케팅 '이달 적립 상위'는 우리가 보낸 적립 기록 기준이다.

## 테스트
`bash test.sh` — 단위 검사(UnitChecks)와 E2E(PointsE2ETest). 벤더 원장은 테스트마다 임시 디렉터리(`PTSCTL_HOME`)에 둔다.
