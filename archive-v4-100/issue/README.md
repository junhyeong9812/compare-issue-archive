# 한빛페이 이슈 아카이브 — 인덱스

과거 장애·운영 이슈 카드 모음. 한 줄 = 카드 하나 (도구/시스템 · 그때 겪은 증상 · 경로).
각 카드 폴더: `1-question.md`(질문) · `2-summary.md`(요약·힌트) · `3-answer.md`(정답·대처) · `metadata.md`.

| 도구/시스템 | 그때 겪은 증상 | 카드 |
|---|---|---|
| acqfeed (카드사 매입 파일 수집) | 카드사가 매입 정정분을 보냈다는데 정산에는 정정 전 금액이 그대로 남음 | `acqfeed/correction-file-glob-miss/` |
| alimgw (알림톡 발송 게이트웨이) | 결제 완료 알림톡이 일부 고객에게 문자로 오거나 아예 오지 않음, 접수 응답은 정상 | `alimgw/template-render-mismatch-sms-fallback/` |
| amlscan (자금세탁방지 모니터링) | 의심거래 보고 파일 제출 응답은 정상인데 다음 날 FIU가 미접수라고 연락 | `amlscan/str-file-bom-reject/` |
| auditsink (감사 로그 수집) | 새벽 6시~9시 사이 관리자 작업 감사 로그만 비어 있음 | `auditsink/naive-ts-closed-index/` |
| batchd (사내 배치 실행기) | 정산 배치는 '성공'인데 일부 가맹점 정산 파일이 비어 있어 대사 차액 | `batchd/pipeline-exit-masked-empty-file/` |
| billkey (정기결제 빌링키) | 정기결제 일괄 청구 날 오전에 결제 실패가 대량으로 몰림 | `billkey/batch-threads-pg-ip-throttle/` |
| bizmsg (메시지 중계) | 예약한 알림톡이 다음 날 아침에 한꺼번에 도착 | `bizmsg/ad-template-night-hold/` |
| blobgw (오브젝트 스토리지 게이트웨이) | 파트너가 일별 거래 파일이 버킷에 없다고 함, 업로드 작업 로그는 성공 | `blobgw/multipart-complete-not-called/` |
| bnkfirm (펌뱅킹 이체 중계) | 은행 전용선 이체가 하루 세 번 정도 몇 분씩 전부 실패 | `bnkfirm/ipsec-rekey-lifetime-mismatch/` |
| bnkfirm (펌뱅킹 이체 중계) | 송금 대사에서 은행 출금이 사내 이체 기록보다 많음 | `bnkfirm/timeout-unknown-treated-failed/` |
| cartsvc (장바구니) | 일부 고객의 장바구니가 갑자기 비어 있다는 문의가 매일 조금씩 들어옴 | `cartsvc/ttl-set-on-create-active-cart-expire/` |
| cdcpipe (DB 변경 데이터 캡처 파이프라인) | 통계 DB의 일 거래 건수가 원장과 몇천 건 차이 나는 날이 생김 | `cdcpipe/slot-recreated-snapshot-never-gap/` |
| cfgsvc (설정 서비스) | 운영 콘솔에서 설정 저장이 30초 걸리다 오류가 나고 다시 누르면 됨 | `cfgsvc/leader-failover-write-timeout/` |
| cfgsvc (설정 서비스) | 운영 콘솔에서 한도를 바꾼 직후 확인하면 기존 가맹점만 '반영 실패', 다음 날 아침엔 정상 | `cfgsvc/set-hidden-until-mark/` |
| chgback (차지백 관리) | 고객센터 상담원 계정으로 차지백 수용 처리가 된 기록이 발견됨 | `chgback/rbac-wildcard-cs-approve/` |
| cipipe (CI 파이프라인) | 통합 테스트가 2주간 계속 실패했는데 머지와 배포가 막히지 않음 | `cipipe/allow-failure-integration-stage/` |
| cmsdebit (CMS 자동이체 출금) | 자동이체 수납 대사에서 회원 납부액 합계가 실제 입금보다 큼 | `cmsdebit/partial-debit-result-code/` |
| connpx (DB 커넥션 프록시) | 프록시 전환 직후 승인 API가 간헐적으로 'prepared statement does not exist' 오류 | `connpx/prepared-stmt-txn-pool-not-exist/` |
| connpx (DB 커넥션 프록시) | 일별 정산 합계가 카드사 대사와 자정 근처 몇십 건만큼 차이 | `connpx/txn-pool-session-timezone-leak/` |
| cpnengine (쿠폰 엔진) | 1만 장 한정 선착순 쿠폰이 1만 장보다 많이 발급되고 사용까지 됨 | `cpnengine/first-come-issue-race-overissue/` |
| cronhub (배치 스케줄러) | 스케줄러 재기동 뒤 리포트 메일이 3중 발송 | `cronhub/misfire-catchup-duplicate-run/` |
| csdesk (고객센터 상담 도구) | 상담 티켓 야간 이관에서 매일 몇 건씩 누락, 다시 돌리면 다른 건이 빠짐 | `csdesk/ticket-offset-pagination-skip/` |
| dbsnap (DB 스냅샷) | 주 중반 시점으로 복구가 안 됨 | `dbsnap/wal-retention-shorter-than-snapshot/` |
| dnsops (사내 DNS) | 가맹점 웹훅 발송은 성공으로 찍히는데 3분의 1 정도만 가맹점이 못 받았다고 함 | `dnsops/round-robin-stale-ip-no-fallback/` |
| edgecache (CDN) | 약관 개정 후 앱 웹뷰에만 옛 약관이 보임 | `edgecache/purge-exact-path-query-key/` |
| evstream (사내 이벤트 스트림 브로커) | 결제 적립 소비자 장애 복구 후 일부 기간 결제의 포인트가 영영 반영되지 않음 | `evstream/lag-past-retention-offset-reset-latest/` |
| evstream (사내 이벤트 스트림 브로커) | 결제 서비스 배포 직후 정산·통계·알림 소비자가 일제히 역직렬화 오류 | `evstream/schema-compat-none-type-widen/` |
| filegw (파트너 파일 게이트웨이) | 파트너가 정산 파일을 못 받았다고 함, 접수 응답은 정상 | `filegw/relay-cert-reload-retry-expire/` |
| flagkit (플래그 SDK) | 플래그를 바꿔도 일부 기능에만 반영이 안 됨 | `flagkit/key-case-default-fallback/` |
| fxq (환율 CLI) | 월말 사흘간 외화 평가 리포트에 일부 통화만 나오고 나머지 통화 칸이 비어 있음 | `fxq/monthly-quota-exhausted-dashboard-poll/` |
| fxq (환율 CLI) | 외화지갑 일일 평가손익이 회계 원장과 연휴 날짜만 차이 | `fxq/tilde-quote-requery/` |
| glsync (회계 GL 연동) | 월말 마감 날에만 회계 연동 배치가 중간에 죽고 재실행해도 같은 지점에서 죽음 | `glsync/month-end-oom-full-load/` |
| glsync (회계 GL 연동) | 일 마감 대사에서 정산 원장과 회계 총계정원장 금액이 일부 날만 어긋남 | `glsync/unbalanced-journal-silent-skip/` |
| hookin (PG·외부 통지 웹훅 수신기) | PG 통지의 약 2%가 서명 불일치로 거절됨, PG사는 서명이 맞다고 함 | `hookin/reserialized-body-hmac-mismatch/` |
| hookout (가맹점 웹훅 발송기) | 모의해킹에서 가맹점 웹훅 URL로 내부 메타데이터 주소가 등록·호출됨 | `hookout/ssrf-link-local-metadata/` |
| iamctl (사내 권한 관리 CLI) | 권한을 회수한 상담원 계정이 회수 후 한 시간 가까이 고객 정보를 계속 조회 | `iamctl/revoke-claims-in-token/` |
| idchk (본인확인 CLI) | 월간 가맹점주 재확인에서 일부 가맹점이 '재확인 미완료'로 남아 정산 보류 | `idchk/batch-head-twelve/` |
| idchk (본인확인 CLI) | 월간 재확인 배치에서 일요일 새벽에 처리된 가맹점만 '재확인 미완료'로 남음 | `idchk/vendor-maintenance-window-timeout/` |
| kdeploy (쿠버네티스 배포 도구) | 배포할 때마다 1~2분간 결제 승인 API 5xx와 지연 급증 | `kdeploy/readiness-before-cache-warmup/` |
| kdeploy (쿠버네티스 배포 도구) | 같은 버전으로 배포했는데 가맹점 정산 금액이 파드에 따라 1원씩 다름 | `kdeploy/reused-tag-ifnotpresent-mixed-image/` |
| kvcache (Redis 캐시 클러스터) | 캐시 노드 증설 작업 중 세션 조회 오류가 20분간 급증 | `kvcache/resharding-ask-redirect-unhandled/` |
| kycflow (고객확인 심사 워크플로) | 벤더는 심사 완료 통보를 보냈다는데 고객 상태가 '심사 중'에 머묾 | `kycflow/webhook-301-post-to-get/` |
| lbctl (L7 로드밸런서) | 한가한 시간대에 가맹점 API 호출이 간헐적으로 502 | `lbctl/idle-timeout-longer-than-upstream/` |
| lbctl (L7 로드밸런서) | 정산 지급 요청이 은행으로 두 번 나간 건이 월말에만 몇 건씩 생겨 대사 차액 | `lbctl/retry-on-timeout-duplicate-post/` |
| ledgerctl (원장 CLI) | 환불 배치 — 영수 번호는 받았는데 입금이 없다는 문의, 환불 대사 차액 | `ledgerctl/memo-byte-silent-hold/` |
| ledgerctl (원장 CLI) | 배치 서버 재이미징 후 첫 새벽 원장 기장 배치가 인증 오류로 전부 실패 | `ledgerctl/reimage-config-owner-root/` |
| logfwd (로그 수집) | 로그 회전 직후 수집 구간이 비어 있음 | `logfwd/copytruncate-offset-gap/` |
| mailrelay (사내 메일 발송 릴레이) | DKIM 키 교체 직후 두 시간 동안 수신 측 인증 결과가 dkim=fail | `mailrelay/dkim-selector-rotate-before-dns/` |
| mailrelay (사내 메일 발송 릴레이) | 명세서 메일이 발송 로그는 정상인데 일부 메일 서비스 고객에게만 스팸함으로 감 | `mailrelay/spf-lookup-limit-permerror/` |
| maskd (개인정보 마스킹 프록시) | 개인정보 점검에서 상담 화면 오류 팝업에 주민등록번호 뒷자리가 그대로 노출 | `maskd/content-type-bypass/` |
| maskd (개인정보 마스킹 프록시) | 파트너 정산 리포트 대사에서 일부 거래가 '미매칭'으로 남음 | `maskd/txid-pattern-masked-recon/` |
| mqpub (메시지 발행) | 일부 가맹점이 정산 완료 웹훅을 못 받음 | `messaging/mqpub-batch-key-head/` |
| mqpub (메시지 발행) | 정산 완료 웹훅이 오전 내내 오지 않다가 오후에 한꺼번에 도착 | `messaging/mqpub-credential-rotation-retry-backlog/` |
| migrun (스키마 마이그레이션 러너) | 마이그레이션 배포 중 결제 승인 테이블 쓰기가 7분간 멈춤 | `migrun/add-column-default-rewrite-lock/` |
| mlgsettle (제휴 마일리지 정산) | 일별 마일리지 합계가 맞지 않고 다음 날 반대 부호로 상쇄 | `mlgsettle/partner-cutoff-2330-window/` |
| mtragent (메트릭 에이전트) | 신규 가맹점 대시보드가 비어 있고 경보도 없음 | `mtragent/series-cap-new-merchant-blank/` |
| nodepool (노드 오토스케일러) | 오후에 가맹점 어드민 전체가 12분간 접속 불가 | `nodepool/spot-only-single-zone-outage/` |
| notifyhub (앱 푸시 발송 게이트웨이) | 앱을 다시 설치한 고객 일부가 결제 알림 푸시를 받지 못함, 발송 결과는 성공 | `notifyhub/device-token-dedup-created-at/` |
| notifyhub (앱 푸시 발송 게이트웨이) | 전 회원 대상 공지 푸시가 다 나가는 데 40분이 걸림 | `notifyhub/http2-single-connection-stream-cap/` |
| ocrid (신분증 진위확인) | 일부 고객의 본인확인이 '진행 중'에서 멈춤 | `ocrid/callback-underscore-header-drop/` |
| oidcbr (소셜 로그인 브로커) | 새벽부터 한 소셜 로그인이 전부 실패, 오후에 저절로 복구 | `oidcbr/jwks-unknown-kid-no-refetch/` |
| ordhub (주문 허브) | 오픈마켓 주문 일부가 주문 허브에 들어오지 않았는데 마켓 쪽에는 주문이 있음 | `ordhub/webhook-secret-rotation-signature-reject/` |
| otpsvc (인증번호·OTP 서버) | 저녁 7시 무렵 인증번호 문자가 5~10분 늦게 와서 입력 시간 초과 | `otpsvc/otp-shared-bulk-queue/` |
| outboxd (아웃박스 이벤트 릴레이) | 연말 거래 증가 후 이벤트 발행 지연이 15분까지 늘고 DB CPU 상승 | `outboxd/dead-tuple-bloat-poll-slow/` |
| outboxd (아웃박스 이벤트 릴레이) | 결제는 완료됐는데 일부 결제 이벤트가 하류 시스템에 영영 도착하지 않음 | `outboxd/sequence-commit-order-skip/` |
| payoutd (가맹점 지급 대행) | 내부 감사에서 수동 지급 건 일부가 결재자 없이 실행된 것으로 보임 | `payoutd/approver-equals-requester/` |
| pgroute (PG 라우팅 게이트웨이) | 환불 요청은 접수됐는데 일부 고객에게 환불이 며칠째 안 들어옴 | `pgroute/cancel-routed-to-other-pg/` |
| pgroute (PG 라우팅 게이트웨이) | 할인 행사 시작 직후 결제 타임아웃이 폭증하고 PG 쪽은 정상이라고 함 | `pgroute/hikari-pool-exhaust-pg-latency/` |
| pgsim (PG 환불) | 정기배송 해지 환불이 고객 계좌에 이틀 늦게 들어갔다는 민원 | `pgsim/merchant-api-key-expiry-refund-delay/` |
| pgsim (PG 환불) | 정기배송 해지·합배송 환불 후 PG 정산 대사에서 환불액 부족 | `pgsim/refund-batch-same-line/` |
| ptsctl (포인트 CLI) | 캐시백→포인트 이관 후 일부 회원 포인트 누락 문의 | `ptsctl/earn-member-leading-zero/` |
| ptsctl (포인트 CLI) | 인프라 작업 다음 날 아침부터 포인트 적립이 전부 '접근 거부'로 실패 | `ptsctl/nat-gateway-egress-ip-allowlist/` |
| pushq (푸시 예약) | 쿠폰 만료 알림이 iOS 고객에게만 사흘째 오지 않음 | `pushq/apns-auth-cert-expiry-ios-only/` |
| pushq (푸시 예약) | 쿠폰 만료 1시간 전 알림이 거의 오지 않음 | `pushq/schedule-offslot-silent-ticket/` |
| pwreset (비밀번호 재설정 서비스) | 재설정 메일 링크를 처음 눌러도 '만료된 링크'로 나옴, 법인 고객 위주 | `pwreset/mail-scanner-consumes-token/` |
| reconsvc (카드 거래 대사) | 카드사 대사에서 일부 거래가 엉뚱한 날짜 거래와 짝지어져 차액이 생김 | `reconsvc/approval-no-wraparound-mismatch/` |
| remitx (해외송금) | 해외 셀러 정산 송금이 일부 금액만 도착했다는 문의 | `remitx/sha-charge-intermediary-deduct/` |
| replwatch (DB 복제 관리) | 가맹점 어드민에서 정산 계좌를 바꿨는데 몇 분간 옛 계좌가 보임 | `replwatch/replica-apply-paused-by-report-query/` |
| sagaorc (주문 사가 오케스트레이터) | 한정판 판매 오픈 10분 동안 주문 타임아웃이 40%까지 오름 | `sagaorc/lock-order-inversion-deadlock/` |
| sanctlist (제재 명단 조회) | 제재 명단 갱신이 10월부터 반영되지 않음, 적재 배치는 매일 성공 | `sanctlist/version-string-compare/` |
| secbox (비밀 저장소) | 한 달에 한 번꼴로 새벽에 정산 서비스만 DB 인증 실패 | `secbox/lease-max-ttl-renew/` |
| secbox (비밀 저장소) | 새 결제 서명 키 적용 후 운영에서만 서명 검증 실패, 스테이징은 정상 | `secbox/pem-env-first-line/` |
| settlebat (가맹점 정산 배치) | 정산 배치가 중간에 'No space left on device'로 멈춤 | `settlebat/tmp-disk-full-batch-abort/` |
| smsgw (SMS 발송 게이트웨이) | 결제 인증 문자가 접수 응답은 정상인데 고객에게 도착하지 않음 | `smsgw/sender-number-dereg-dlr-3008/` |
| splitab (A/B 테스트) | 50:50으로 나눈 A/B 테스트 집계 인원이 한쪽으로 4%p 기울어 판독이 멈춤 | `splitab/bot-filter-arm-asymmetry-srm/` |
| srchidx (검색엔진 색인 파이프라인) | 거래 검색에서 일부 거래만 빠지고 색인 배치는 매번 성공 | `srchidx/bulk-200-partial-errors-ignored/` |
| ssogw (사내 SSO) | 로그인이 간헐적으로 실패하고 다시 하면 됨 | `ssogw/saml-clock-skew-login-loop/` |
| stockpush (재고 연동) | 주말 동안 오픈마켓 재고가 갱신되지 않아 품절 상품 주문이 들어옴 | `stockpush/openmall-token-refresh-cron-missing/` |
| stockpush (재고 연동) | 창고 이전 때 판매를 멈췄는데 주문이 계속 들어옴 | `stockpush/zero-qty-noop/` |
| tickd (분산 작업 스케줄러) | 이자 지급 안내가 일부 고객에게 두 번 발송됨 | `tickd/leader-lease-gc-pause-double-run/` |
| tlsrenew (인증서 자동 갱신) | 인증서 갱신 다음 날부터 일부 파트너사만 우리 API 호출이 TLS 오류 | `tlsrenew/missing-intermediate-java-partners/` |
| tokensvc (토큰 발급 서버) | 앱을 오래 두었다 열면 간헐적으로 강제 로그아웃 | `tokensvc/refresh-rotation-race/` |
| tokvault (카드 토큰 보관) | 오래된 정기결제 회원만 결제 시 '토큰 복호화 실패' 오류 | `tokvault/key-rotation-old-key-disabled-early/` |
| tracepipe (분산 트레이싱 수집기) | 트레이스 백엔드 장애 뒤 한 시간치 스팬이 일부만 들어와 있음, 수집기는 정상 응답 | `tracepipe/exporter-queue-full-silent-drop/` |
| vacct (가상계좌 발급·입금 통지) | 고객은 가상계좌로 입금했다는데 주문이 입금 대기에서 안 넘어감 | `vacct/strict-amount-mismatch-park/` |
| vanrelay (VAN 중계) | 부분취소 건의 부가세 대사만 어긋남 | `vanrelay/partial-cancel-vat-zero/` |
| wafedge (웹 방화벽) | 대형 가맹점 한 곳만 점심 피크에 결제 API가 429로 실패 | `wafedge/nat-rate-limit/` |
| wmsbridge (창고 WMS 연동) | 당일 출고 약속 주문 일부가 다음 날 출고돼 배송 지연 보상 요청 | `wmsbridge/wave-cutoff-queue-lag-next-day-ship/` |
| xferq (파일 전송 CLI) | 카드사에 다시 보낸 대사 파일을 카드사가 못 받았다고 함 | `xferq/name-underscore-silent-park/` |
| xferq (파일 전송 CLI) | 파일 전송 배치가 '토큰 유효 시간' 오류로 매시간 실패 | `xferq/ntp-stopped-token-not-yet-valid/` |
