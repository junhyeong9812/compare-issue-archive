# 한빛몰 환불 데스크 (refund-desk)

한빛몰 주문의 결제·부분 환불을 PG(결제대행사)와 처리하는 백오피스 서비스다.
CS가 반품 확인된 상품 단위로 환불을 접수하면, 매일 밤 환불 실행이 접수분을 모아 PG 환불 배치로 보내고
PG 환불 번호를 받아 고객 환불 내역에 반영한다.

## 구성
```
src/api       RefundDeskApi(퍼사드 — CS 콘솔·야간 스케줄러가 호출), StatementFormatter
src/service   CheckoutService, RefundService, RefundBatchJob, RefundResultMatcher,
              CustomerStatementService, DiscountAllocator
src/infra     PgsimClient(PG 벤더 CLI 호출), RefundFileWriter(환불 배치 파일), CatalogLoader
src/store     주문·환불 저장소(인메모리)
src/domain    레코드 타입
src/util      Money, Refs
conf/         app.properties (DESK_* 환경변수 / -Ddesk.* 로 덮어쓰기)
fixtures/     상품 카탈로그
vendor/bin/   PG 벤더 제공 CLI `pgsim` (벤더 배포본 — 저장소에서 수정하지 않는다. `pgsim --help`)
docs/runbook/ 운영 런북
```

## 규칙
- 주문 상품은 1개 = 1줄이다. 같은 상품을 2개 사면 줄이 2개다.
- 주문 쿠폰은 상품 가격 비율로 줄마다 나눠 붙이고(원 미만 버림, 남는 원은 가장 비싼 첫 줄), 부분 환불은 그 줄의 실결제액이다.
- 환불 실행 1회 = 회차 번호(`RB<날짜>-<회차>`). PG 배치 번호는 회차 번호 + 청크 번호이고 청크당 PG 호출은 1번이다.
  PG 정산 대사는 배치 번호 단위로 한다.
- 고객 환불 내역의 결제·환불·잔액 금액은 PG 조회 값이다(우리 장부 값이 아니다).

## 테스트
`bash test.sh` — 단위 검사(UnitChecks)와 E2E(RefundE2ETest). PG 상태는 테스트마다 임시 디렉터리(`PGSIM_HOME`)에 둔다.
