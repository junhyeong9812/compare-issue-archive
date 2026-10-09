# 한빛페이 의사결정 기록 — 인덱스

과거 설계·운영 결정 기록 모음이다. 한 줄이 기록 하나다(시스템 · 다룬 주제 · 경로).
각 기록 폴더에는 다음 파일이 있다.
- `1-question.md`: 당시 맥락
- `2-summary.md`: 결정 요약
- `3-answer.md`: 이유·검토한 대안·결과·이후 변경 시 주의
- `metadata.md`

| 시스템 | 다룬 주제 | 기록 |
|---|---|---|
| 자동이체 알림 | 출금 실패 뒤 재출금 안내 발송 | `autopay-notify/bank-refeed-retry-notice/` |
| bizmsg (메시지 중계) | 알림톡 실패 시 SMS 대체 발송 | `bizmsg/sms-fallback-scope/` |
| bizmsg (메시지 중계) | 알림톡 발송 타임아웃 처리 | `bizmsg/timeout-no-blind-resend/` |
| dbsnap (DB 스냅샷) | 운영 DB 스냅샷 주기와 보존 기간 | `dbsnap/daily-snapshot-14d-retention/` |
| escrowpay (에스크로) | 자동 구매확정 시점 | `escrowpay/auto-confirm-delivery-d8/` |
| filegw (파트너 파일 게이트웨이) | 제휴사 파일 재전송 | `filegw/no-auto-resend-after-ack/` |
| fxwallet (외화지갑) | 평가 환율 조회 주기 | `fxwallet/valuation-rate-once-daily/` |
| hbexpense (경비 정산) | 경비 포털 화면의 원장 조회 | `hbexpense/claim-detail-ledger-lookup-removed/` |
| logfwd (로그 수집) | 결제 API 디버그 로그 수집 범위 | `logfwd/debug-log-sampled-ingest/` |
| mallsearch (포인트몰 검색) | 검색 색인의 재고 반영 주기 | `mallsearch/stock-reindex-hourly/` |
| 포인트몰 재고 | 오픈마켓 판매 재개 동기화 | `market-stock/restock-alert-warning/` |
| 회원 가입 | 가입 연령과 정회원 전환 | `onboarding/signup-age-and-activation/` |
| 운영 콘솔 | 고객 정보 원문 조회 | `opsconsole/customer-lookup-ticket-link/` |
| 주문 알림 | 실패한 알림 문자 재발송 | `order-alerts/carrier-reject-no-resend/` |
| ovsettle (해외카드 정산) | 환율 벤더 값의 외부 제공 | `ovsettle/fxq-b2-values-stay-internal/` |
| payout (파트너 정산 파일) | 수신 확인 리포트 표시 | `payout/split-day-receipts-all-parts/` |
| 멤버십 포인트 | 보상·수기 조정 포인트와 적립 순위 | `pointsvc/manual-points-and-ranking/` |
| pushq (앱 푸시) | 마케팅 푸시 재시도 | `pushq/marketing-push-retry-cap/` |
| refunddesk (환불 데스크) | 부분 환불을 PG로 보내는 시점 | `refunddesk/same-day-payment-refund-hold/` |
| rollout (설정 배포) | 서버 설정 파일 갱신 방식 | `rollout/unchanged-host-config-kept/` |
| ssogw (사내 SSO) | 운영 콘솔 위험 작업 인증 | `ssogw/admin-console-step-up-reauth/` |
| vanrelay (VAN 중계) | 매입 대사 파일의 수수료 | `vanrelay/acquirer-fee-as-given/` |
