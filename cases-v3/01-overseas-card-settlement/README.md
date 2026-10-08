# 한빛페이 해외카드 파트너 정산 (overseas-card-settlement)

해외 가맹 파트너의 카드 승인 건을 원화로 환산해 일자·파트너별로 정산하고,
파트너가 보내 주는 명세와 대사(reconcile)하는 배치/운영 콘솔이다.

## 구성
```
src/api       SettlementApi(퍼사드), ReportFormatter, Main(운영 CLI)
src/service   SettlementService, ReconcileService, RateService, RateBoardService, ...
src/infra     FxqClient(환율 벤더 CLI 호출), DataFiles(CSV)
src/domain    레코드 타입
src/util      Money, Csv
conf/         app.properties (SETTLE_* 환경변수 / -Dsettle.* 로 덮어쓰기)
fixtures/     샘플 데이터 (2026-10-01 ~ 10-05)
vendor/bin/   환율 벤더 제공 CLI `fxq` (벤더 배포본 — 저장소에서 수정하지 않는다)
```

## 실행
```
JDK=$HOME/.sdkman/candidates/java/21.0.5-tem/bin
$JDK/javac -encoding UTF-8 -d out $(find src -name '*.java')
$JDK/java -cp out api.Main report    2026-10-01 2026-10-05
$JDK/java -cp out api.Main reconcile 2026-10-01 2026-10-05
$JDK/java -cp out api.Main export    2026-10-01 2026-10-05
$JDK/java -cp out api.Main board     2026-10-03
```

## 규칙
- 승인 시각은 UTC로 들어오고, 정산 일자는 KST 달력 날짜다.
- 원화 환산은 거래 일자의 환율로, 원 단위 반올림(HALF_UP). JPY는 100엔당 환율.
- 수수료는 일자·파트너 합계 원화에 bps를 곱해 원 미만 절사. 환불은 음수 금액으로 합산.
- 비활성 파트너는 정산하지 않는다.

## 테스트
`bash test.sh` — 단위 검사(UnitChecks)와 E2E(SettlementE2ETest).
