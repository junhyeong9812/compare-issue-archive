# 한빛페이 이슈 아카이브 — 인덱스

과거 장애·운영 이슈 카드 모음. 한 줄 = 카드 하나 (도구/시스템 · 그때 겪은 증상 · 경로).
각 카드 폴더: `1-question.md`(질문) · `2-summary.md`(요약·힌트) · `3-answer.md`(정답·대처) · `metadata.md`.

| 도구/시스템 | 그때 겪은 증상 | 카드 |
|---|---|---|
| bizmsg (메시지 중계) | 예약한 알림톡이 다음 날 아침에 한꺼번에 도착 | `bizmsg/ad-template-night-hold/` |
| cfgsvc (설정 서비스) | 운영 콘솔에서 한도를 바꾼 직후 확인하면 기존 가맹점만 '반영 실패', 다음 날 아침엔 정상 | `cfgsvc/set-hidden-until-mark/` |
| cronhub (배치 스케줄러) | 스케줄러 재기동 뒤 리포트 메일이 3중 발송 | `cronhub/misfire-catchup-duplicate-run/` |
| dbsnap (DB 스냅샷) | 주 중반 시점으로 복구가 안 됨 | `dbsnap/wal-retention-shorter-than-snapshot/` |
| edgecache (CDN) | 약관 개정 후 앱 웹뷰에만 옛 약관이 보임 | `edgecache/purge-exact-path-query-key/` |
| filegw (파트너 파일 게이트웨이) | 파트너가 정산 파일을 못 받았다고 함, 접수 응답은 정상 | `filegw/relay-cert-reload-retry-expire/` |
| flagkit (플래그 SDK) | 플래그를 바꿔도 일부 기능에만 반영이 안 됨 | `flagkit/key-case-default-fallback/` |
| fxq (환율 CLI) | 외화지갑 일일 평가손익이 회계 원장과 연휴 날짜만 차이 | `fxq/tilde-quote-requery/` |
| idchk (본인확인 CLI) | 월간 가맹점주 재확인에서 일부 가맹점이 '재확인 미완료'로 남아 정산 보류 | `idchk/batch-head-twelve/` |
| ledgerctl (원장 CLI) | 환불 배치 — 영수 번호는 받았는데 입금이 없다는 문의, 환불 대사 차액 | `ledgerctl/memo-byte-silent-hold/` |
| logfwd (로그 수집) | 로그 회전 직후 수집 구간이 비어 있음 | `logfwd/copytruncate-offset-gap/` |
| mlgsettle (제휴 마일리지 정산) | 일별 마일리지 합계가 맞지 않고 다음 날 반대 부호로 상쇄 | `mlgsettle/partner-cutoff-2330-window/` |
| mqpub (메시지 발행) | 일부 가맹점이 정산 완료 웹훅을 못 받음 | `messaging/mqpub-batch-key-head/` |
| mtragent (메트릭 에이전트) | 신규 가맹점 대시보드가 비어 있고 경보도 없음 | `mtragent/series-cap-new-merchant-blank/` |
| ocrid (신분증 진위확인) | 일부 고객의 본인확인이 '진행 중'에서 멈춤 | `ocrid/callback-underscore-header-drop/` |
| pgsim (PG 환불) | 정기배송 해지·합배송 환불 후 PG 정산 대사에서 환불액 부족 | `pgsim/refund-batch-same-line/` |
| ptsctl (포인트 CLI) | 캐시백→포인트 이관 후 일부 회원 포인트 누락 문의 | `ptsctl/earn-member-leading-zero/` |
| pushq (푸시 예약) | 쿠폰 만료 1시간 전 알림이 거의 오지 않음 | `pushq/schedule-offslot-silent-ticket/` |
| ssogw (사내 SSO) | 로그인이 간헐적으로 실패하고 다시 하면 됨 | `ssogw/saml-clock-skew-login-loop/` |
| stockpush (재고 연동) | 창고 이전 때 판매를 멈췄는데 주문이 계속 들어옴 | `stockpush/zero-qty-noop/` |
| vanrelay (VAN 중계) | 부분취소 건의 부가세 대사만 어긋남 | `vanrelay/partial-cancel-vat-zero/` |
| xferq (파일 전송 CLI) | 카드사에 다시 보낸 대사 파일을 카드사가 못 받았다고 함 | `xferq/name-underscore-silent-park/` |
