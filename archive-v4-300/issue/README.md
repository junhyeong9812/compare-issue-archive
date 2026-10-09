# 한빛페이 이슈 아카이브 — 인덱스

과거 장애·운영 이슈 카드 모음. 한 줄 = 카드 하나 (도구/시스템 · 그때 겪은 증상 · 경로).
각 카드 폴더: `1-question.md`(질문) · `2-summary.md`(요약·힌트) · `3-answer.md`(정답·대처) · `metadata.md`.

| 도구/시스템 | 그때 겪은 증상 | 카드 |
|---|---|---|
| acqfeed (카드사 매입 파일 수집) | 카드사가 매입 정정분을 보냈다는데 정산에는 정정 전 금액이 그대로 남음 | `acqfeed/correction-file-glob-miss/` |
| acqfeed (카드사 매입 파일 수집) | 보안 점검에서 수집 서버 로그에 카드번호 원문이 발견됨 | `acqfeed/debug-log-pan-plaintext/` |
| acqfeed (카드사 매입 파일 수집) | 일부 가맹점만 매입 대사에서 '매칭 실패'로 남고 수기로 맞추면 금액은 같음 | `acqfeed/euc-kr-merchant-name-match/` |
| acqfeed (카드사 매입 파일 수집) | 새벽 매입 수집 배치가 전 카드사 중 한 곳만 연속 실패 | `acqfeed/sftp-hostkey-rotation-fail/` |
| alertrt (경보 라우터) | 승인 오류율이 10%를 넘었는데 당직 호출이 오지 않음 | `alertrt/inhibit-by-stale-cluster-alert/` |
| alertrt (경보 라우터) | 정산 파일 생성 실패 경보가 인프라 채널에만 가고 정산팀은 못 받음 | `alertrt/route-first-match-team-not-paged/` |
| alimgw (알림톡 발송 게이트웨이) | 영수증 조회 도메인을 옮긴 배포 직후 영수증 알림톡이 전부 오류 응답 | `alimgw/button-link-domain-not-registered/` |
| alimgw (알림톡 발송 게이트웨이) | 해외송금 서비스 재개 첫날 알림톡이 전부 실패 응답 | `alimgw/sender-profile-dormant-block/` |
| alimgw (알림톡 발송 게이트웨이) | 결제 완료 알림톡이 일부 고객에게 문자로 오거나 아예 오지 않음, 접수 응답은 정상 | `alimgw/template-render-mismatch-sms-fallback/` |
| amlscan (자금세탁방지 모니터링) | 분기 점검에서 FIU가 지난달 보고 일부를 미제출로 집계, 내부 제출 기록은 다 있음 | `amlscan/ack-pickup-void/` |
| amlscan (자금세탁방지 모니터링) | 고액현금거래 보고 건수가 회계팀 집계보다 매달 몇십 건 많음 | `amlscan/ctr-cancel-not-netted/` |
| amlscan (자금세탁방지 모니터링) | 새 분할송금 탐지 룰이 배포 후 2주간 경보를 한 건도 내지 않음 | `amlscan/rule-window-unit/` |
| amlscan (자금세탁방지 모니터링) | 의심거래 보고 파일 제출 응답은 정상인데 다음 날 FIU가 미접수라고 연락 | `amlscan/str-file-bom-reject/` |
| artihub (아티팩트 저장소) | 노드 교체 후 정산 대사 서비스 파드가 이미지를 못 받아 기동 실패 | `artihub/cleanup-unused-30d-deleted-prod-image/` |
| artihub (아티팩트 저장소) | 공개된 지 몇 시간 지난 라이브러리 새 버전을 사내 빌드에서 하루 종일 못 받음 | `artihub/remote-404-cached-new-release/` |
| auditsink (감사 로그 수집) | 외주 운영자 계정의 감사 이벤트가 날에 따라 검색되지 않음 | `auditsink/field-mapping-conflict-drop/` |
| auditsink (감사 로그 수집) | 새벽 6시~9시 사이 관리자 작업 감사 로그만 비어 있음 | `auditsink/naive-ts-closed-index/` |
| auditsink (감사 로그 수집) | 배포가 있던 날만 감사 로그 건수가 애플리케이션 로그보다 적음 | `auditsink/shutdown-buffer-no-flush/` |
| batchd (사내 배치 실행기) | 새벽 배치를 컨테이너로 옮긴 뒤 산출 파일명이 하루 전 날짜로 생성됨 | `batchd/container-utc-file-date/` |
| batchd (사내 배치 실행기) | 월말 수수료 집계 배치가 OutOfMemoryError로 세 번 연속 실패 | `batchd/month-end-full-list-oom/` |
| batchd (사내 배치 실행기) | 정산 배치는 '성공'인데 일부 가맹점 정산 파일이 비어 있어 대사 차액 | `batchd/pipeline-exit-masked-empty-file/` |
| batchd (사내 배치 실행기) | 포인트 소멸 배치가 중단 후 재시작해 '완료'됐는데 일부 회원 포인트가 소멸되지 않음 | `batchd/restart-checkpoint-read-offset-skip/` |
| bidash (BI 대시보드) | 데이터팀이 매출을 정정했는데 경영 대시보드에는 하루 종일 옛 숫자가 나옴 | `bidash/exec-dashboard-cache-ttl-correction/` |
| bidash (BI 대시보드) | 조직 개편 첫날 일부 팀 대시보드가 오류 없이 빈 화면으로 나옴 | `bidash/row-permission-group-reorg-blank/` |
| billkey (정기결제 빌링키) | 정기결제 일괄 청구 날 오전에 결제 실패가 대량으로 몰림 | `billkey/batch-threads-pg-ip-throttle/` |
| billkey (정기결제 빌링키) | 정기결제 일부 회원만 매달 결제 실패가 늘고 다음 달에도 반복 | `billkey/card-reissue-billing-key-invalid/` |
| bizmsg (메시지 중계) | 예약한 알림톡이 다음 날 아침에 한꺼번에 도착 | `bizmsg/ad-template-night-hold/` |
| bkvault (백업 볼트) | 감사 대응으로 4월 말 시점 원장을 복구하려는데 4월 월간 백업이 없음 | `bkvault/gfs-monthly-picks-failed-weekly/` |
| bkvault (백업 볼트) | DB 복구 연습에서 6개월 전 백업만 복호화 실패 | `bkvault/kms-key-version-disabled-restore/` |
| bkvault (백업 볼트) | 정산 서버 복구 연습에서 정산 첨부 디렉터리 일부만 비어 있음 | `bkvault/one-file-system-skips-new-volume/` |
| blobgw (오브젝트 스토리지 게이트웨이) | 파트너가 업로드 완료 통지를 받고 바로 가져가면 파일이 없다고 하다가 몇 분 뒤 성공 | `blobgw/dr-region-read-replication-lag/` |
| blobgw (오브젝트 스토리지 게이트웨이) | 파트너가 일별 거래 파일이 버킷에 없다고 함, 업로드 작업 로그는 성공 | `blobgw/multipart-complete-not-called/` |
| blobgw (오브젝트 스토리지 게이트웨이) | 가맹점에 보낸 7일짜리 정산서 다운로드 링크가 한 시간 만에 만료 오류 | `blobgw/presigned-url-temp-credential-expiry/` |
| bnkfirm (펌뱅킹 이체 중계) | 자정 무렵 가맹점 지급 일부가 수십 분 늦게 입금됨 | `bnkfirm/bank-maintenance-window-queue/` |
| bnkfirm (펌뱅킹 이체 중계) | 은행 전용선 이체가 하루 세 번 정도 몇 분씩 전부 실패 | `bnkfirm/ipsec-rekey-lifetime-mismatch/` |
| bnkfirm (펌뱅킹 이체 중계) | 송금 대사에서 은행 출금이 사내 이체 기록보다 많음 | `bnkfirm/timeout-unknown-treated-failed/` |
| botguard (봇 차단·캡차) | 결제 페이지에서 캡차를 풀어도 계속 다시 나옴 | `botguard/captcha-cookie-subdomain/` |
| botguard (봇 차단·캡차) | 새벽 시간 다수 계정 무단 로그인 민원, 봇 차단 경보는 없음 | `botguard/distributed-stuffing-missed/` |
| botguard (봇 차단·캡차) | 한 통신사 5G 고객 일부가 로그인 때마다 추가 인증을 요구받음 | `botguard/geoip-db-stale/` |
| callrec (상담 녹취 보관) | 민원 조사 중 넉 달 전 상담 녹취가 일부 없음 | `callrec/lifecycle-prefix-typo/` |
| callrec (상담 녹취 보관) | 1시간 넘는 상담 통화는 녹취 후반부가 재생되지 않음 | `callrec/long-call-part-unlinked/` |
| callrec (상담 녹취 보관) | 상담 품질 키워드 검색에서 새 콜센터 상담 건이 거의 잡히지 않음 | `callrec/stt-single-channel/` |
| cartsvc (장바구니) | 로그인하면 장바구니 상품 수량이 두 배가 됨 | `cartsvc/guest-merge-quantity-doubled/` |
| cartsvc (장바구니) | 결제 직전 금액이 장바구니에 보이던 금액과 달라 결제 이탈이 급증 | `cartsvc/promo-end-price-snapshot-mismatch/` |
| cartsvc (장바구니) | 일부 고객의 장바구니가 갑자기 비어 있다는 문의가 매일 조금씩 들어옴 | `cartsvc/ttl-set-on-create-active-cart-expire/` |
| cashrcpt (현금영수증 발행) | 환불한 현금영수증 일부가 국세청에는 발급 상태로 남아 고객 연말정산에 잡힘 | `cashrcpt/cancel-orig-date-today-reject/` |
| cashrcpt (현금영수증 발행) | 배포 직후 현금영수증 발행 오류 로그에 '중복 요청' 응답이 수백 건 | `cashrcpt/sigterm-inflight-duplicate-issue/` |
| cdcpipe (DB 변경 데이터 캡처 파이프라인) | 원장 DB 디스크 사용률이 사흘 새 60%에서 98%로 오름 | `cdcpipe/inactive-slot-wal-disk-full/` |
| cdcpipe (DB 변경 데이터 캡처 파이프라인) | 분석 저장소의 월 수수료 합계가 원장과 수 원~수십 원 차이 나는 대사 차액 | `cdcpipe/numeric-as-double-rounding/` |
| cdcpipe (DB 변경 데이터 캡처 파이프라인) | 통계 DB의 일 거래 건수가 원장과 몇천 건 차이 나는 날이 생김 | `cdcpipe/slot-recreated-snapshot-never-gap/` |
| cdcpipe (DB 변경 데이터 캡처 파이프라인) | 가격을 바꾼 상품 일부의 검색 결과 설명이 '__debezium_unavailable_value'로 보임 | `cdcpipe/toast-unchanged-placeholder-overwrite/` |
| cfgsvc (설정 서비스) | 가맹점 한도 변경이 결제 서버 일부 노드에만 반영되지 않음 | `cfgsvc/client-token-ttl-stale-node-cache/` |
| cfgsvc (설정 서비스) | 운영 콘솔에서 설정 저장이 30초 걸리다 오류가 나고 다시 누르면 됨 | `cfgsvc/leader-failover-write-timeout/` |
| cfgsvc (설정 서비스) | 운영 콘솔에서 한도를 바꾼 직후 확인하면 기존 가맹점만 '반영 실패', 다음 날 아침엔 정상 | `cfgsvc/set-hidden-until-mark/` |
| chatbridge (상담 채팅 중계) | 상담 채팅에서 고객 메시지가 상담원 화면에 늦게 뜸 | `chatbridge/lb-idle-poll-fallback/` |
| chatbridge (상담 채팅 중계) | 상담 종료 후 이력을 다시 열면 첨부 이미지가 깨져 보임 | `chatbridge/presigned-url-expire-history/` |
| chgback (차지백 관리) | 차지백 이의제기 증빙을 올렸는데 카드사가 증빙이 없다고 기각 | `chgback/heic-evidence-unreadable/` |
| chgback (차지백 관리) | 카드사 포털에서 차지백 목록을 가져오는 작업이 매일 로그인 단계에서 실패 | `chgback/portal-crawler-mfa-login/` |
| chgback (차지백 관리) | 고객센터 상담원 계정으로 차지백 수용 처리가 된 기록이 발견됨 | `chgback/rbac-wildcard-cs-approve/` |
| chgback (차지백 관리) | 차지백 환수가 있었던 가맹점 정산 대사에서 금액이 두 배로 어긋남 | `chgback/sign-inverted-chargeback-file/` |
| cipipe (CI 파이프라인) | 통합 테스트가 2주간 계속 실패했는데 머지와 배포가 막히지 않음 | `cipipe/allow-failure-integration-stage/` |
| cipipe (CI 파이프라인) | CI는 통과했는데 운영 배포 후 파트너가 금액 형식 오류로 요청을 거부 | `cipipe/cache-key-missing-lockfile/` |
| cipipe (CI 파이프라인) | 두 서비스의 배포 산출물 버전이 둘 다 0.0.0-unknown으로 찍히고 하나가 다른 하나를 덮어씀 | `cipipe/shallow-clone-version-unknown-overwrite/` |
| cmsdebit (CMS 자동이체 출금) | 해지한 회원에게서 자동이체가 빠져나갔다는 민원 | `cmsdebit/cancel-sync-missing-terminated-member/` |
| cmsdebit (CMS 자동이체 출금) | 자동이체 출금 신청은 접수됐는데 일부 회원만 출금이 안 되고 결과가 늦게 '불능' | `cmsdebit/consent-evidence-unregistered/` |
| cmsdebit (CMS 자동이체 출금) | 자동이체 수납 대사에서 회원 납부액 합계가 실제 입금보다 큼 | `cmsdebit/partial-debit-result-code/` |
| cmsdebit (CMS 자동이체 출금) | 출금 결과 파일 처리가 새벽부터 실패하고 파일은 정상 수신됨 | `cmsdebit/pgp-key-rotation-decrypt-fail/` |
| connpx (DB 커넥션 프록시) | 정산 서비스 배포 후 피크 시간 응답이 3초 이상으로 늘고 DB CPU는 한가함 | `connpx/new-db-user-default-pool-size/` |
| connpx (DB 커넥션 프록시) | 프록시 전환 직후 승인 API가 간헐적으로 'prepared statement does not exist' 오류 | `connpx/prepared-stmt-txn-pool-not-exist/` |
| connpx (DB 커넥션 프록시) | 일별 정산 합계가 카드사 대사와 자정 근처 몇십 건만큼 차이 | `connpx/txn-pool-session-timezone-leak/` |
| consentdb (개인정보 동의 관리) | 문자 수신을 거부한 고객에게 광고 문자가 계속 발송됨 | `consentdb/channel-all-key-optout/` |
| consentdb (개인정보 동의 관리) | 제휴사에 넘긴 마케팅 동의 명단에 이미 철회한 고객이 계속 포함 | `consentdb/export-delta-misses-withdraw/` |
| consentdb (개인정보 동의 관리) | 분쟁 조정 요청 건의 거래 기록이 조회되지 않음 | `consentdb/purge-cascade-tx-records/` |
| cpnengine (쿠폰 엔진) | 판매자 쿠폰 분담금 월간 리포트가 판매자 정산액과 일부 판매자만 맞지 않음 | `cpnengine/burden-report-reissue-double-count/` |
| cpnengine (쿠폰 엔진) | 만료일 당일 오전부터 쿠폰이 사용 불가라는 문의가 몰림 | `cpnengine/expiry-utc-midnight-kst-morning/` |
| cpnengine (쿠폰 엔진) | 1만 장 한정 선착순 쿠폰이 1만 장보다 많이 발급되고 사용까지 됨 | `cpnengine/first-come-issue-race-overissue/` |
| cronhub (배치 스케줄러) | 스케줄러 재기동 뒤 리포트 메일이 3중 발송 | `cronhub/misfire-catchup-duplicate-run/` |
| csdesk (고객센터 상담 도구) | 상담원 화면에서 일부 고객의 2023년 이전 상담 이력이 보이지 않음 | `csdesk/phone-format-history-split/` |
| csdesk (고객센터 상담 도구) | 점심시간마다 상담 도구 고객 검색이 8초 넘게 걸리고 DB CPU가 치솟음 | `csdesk/phone-suffix-like-scan/` |
| csdesk (고객센터 상담 도구) | 상담원이 환불을 승인했는데 고객 환불이 사흘 뒤에야 처리 | `csdesk/refund-approval-no-delegate/` |
| csdesk (고객센터 상담 도구) | 상담 티켓 야간 이관에서 매일 몇 건씩 누락, 다시 돌리면 다른 건이 빠짐 | `csdesk/ticket-offset-pagination-skip/` |
| dbsnap (DB 스냅샷) | 주 중반 시점으로 복구가 안 됨 | `dbsnap/wal-retention-shorter-than-snapshot/` |
| devbind (기기 등록·간편인증) | 지문을 새로 등록한 안드로이드 고객이 생체 로그인을 못 함 | `devbind/biometric-key-invalidated/` |
| devbind (기기 등록·간편인증) | 앱을 재설치한 고객 일부가 간편인증 등록에 실패 | `devbind/reinstall-device-cap/` |
| dlqsweep (DLQ 재처리 도구) | 일부 가맹점의 정산 완료 알림만 6시간 늦게 도착 | `dlqsweep/fifo-group-poison-head-block/` |
| dlqsweep (DLQ 재처리 도구) | 장애 후 DLQ 재처리를 했는데 일부 정산 이벤트가 처리 없이 다시 DLQ에 쌓임 | `dlqsweep/redrive-keeps-retry-count-header/` |
| dnsops (사내 DNS) | 외부 PG 호출 지연이 피크마다 수백 ms 늘고 DNS 서버 CPU가 치솟음 | `dnsops/ndots-search-domain-amplification/` |
| dnsops (사내 DNS) | 새 파트너 엔드포인트를 등록했는데 일부 서버에서만 한 시간 동안 이름을 못 찾음 | `dnsops/negative-cache-before-record-created/` |
| dnsops (사내 DNS) | 가맹점 웹훅 발송은 성공으로 찍히는데 3분의 1 정도만 가맹점이 못 받았다고 함 | `dnsops/round-robin-stale-ip-no-fallback/` |
| edgecache (CDN) | 약관 개정 후 앱 웹뷰에만 옛 약관이 보임 | `edgecache/purge-exact-path-query-key/` |
| egresspx (아웃바운드 프록시) | 환불 결과 통지가 접수는 정상인데 한 카드사만 며칠째 못 받았다고 함 | `egresspx/ip-allowlist-partner-ip-changed/` |
| egresspx (아웃바운드 프록시) | 결제 피크 때 외부 PG 연결이 무작위로 타임아웃, PG 쪽은 요청을 못 봄 | `egresspx/snat-port-exhaustion-peak/` |
| escrowd (에스크로 구매확정) | 에스크로 주문 일부만 구매확정이 안 돼 판매자 정산이 늦어짐 | `escrowd/courier-code-tracking-no-start/` |
| escrowd (에스크로 구매확정) | 배포 시각에 결제 완료 처리가 10분간 멈춤 | `escrowd/enum-migration-table-lock/` |
| etaxbr (전자세금계산서 발행 중계) | 수정세금계산서 일부만 국세청에 반영되지 않아 거래처 부가세 신고와 차이 | `etaxbr/amend-original-ntsno-missing/` |
| etaxbr (전자세금계산서 발행 중계) | 월초 세금계산서 일괄 발행이 전부 인증 오류로 실패 | `etaxbr/api-key-rotation-grace-end/` |
| etaxbr (전자세금계산서 발행 중계) | 세금계산서는 '발행 완료'인데 일부 거래처가 홈택스에서 조회가 안 된다고 함 | `etaxbr/buyer-bizno-hyphen-reject/` |
| etaxbr (전자세금계산서 발행 중계) | 중계사 장애 시간에 가맹점 가입 API까지 같이 느려짐 | `etaxbr/sync-issue-timeout-thread-block/` |
| etlflow (ETL 파이프라인) | 백필 작업 다음 날 어제 매출이 절반으로 줄어 보임 | `etlflow/backfill-partition-overwrite-today-loss/` |
| etlflow (ETL 파이프라인) | 행사 다음 날 아침 매출 대시보드가 비어 있음 | `etlflow/peak-volume-join-oom-empty-dashboard/` |
| etlflow (ETL 파이프라인) | 일 매출 집계에서 일부 채널 매출만 0으로 나오고 전체 매출이 줄어 보임 | `etlflow/source-column-rename-null-revenue/` |
| evstream (사내 이벤트 스트림 브로커) | 해지한 가맹점 일부가 가맹점 검색에 계속 노출됨 | `evstream/compacted-tombstone-expired-before-consume/` |
| evstream (사내 이벤트 스트림 브로커) | 결제 적립 소비자 장애 복구 후 일부 기간 결제의 포인트가 영영 반영되지 않음 | `evstream/lag-past-retention-offset-reset-latest/` |
| evstream (사내 이벤트 스트림 브로커) | 이상거래 점수 산출이 20분간 멈추고 소비자 로그에 리밸런싱만 반복됨 | `evstream/max-poll-interval-rebalance-storm/` |
| evstream (사내 이벤트 스트림 브로커) | 결제 완료된 주문 일부가 조회 화면에 '결제 대기'로 남음 | `evstream/null-key-partition-reorder/` |
| evstream (사내 이벤트 스트림 브로커) | 결제 서비스 배포 직후 정산·통계·알림 소비자가 일제히 역직렬화 오류 | `evstream/schema-compat-none-type-widen/` |
| feecalc (가맹점 수수료 계산) | 해외카드 수수료 정산 대사에서 카드사 청구액과 매달 소액 차이 | `feecalc/foreign-card-fx-auth-date/` |
| feecalc (가맹점 수수료 계산) | 정산 배치가 평소 40분에서 4시간 넘게 걸려 지급 시각을 놓침 | `feecalc/n-plus-one-rate-lookup/` |
| feecalc (가맹점 수수료 계산) | 반기 우대 수수료 갱신 후 일부 가맹점만 수수료가 예전 요율로 계산됨 | `feecalc/preferential-rate-rep-merchant-only/` |
| filegw (파트너 파일 게이트웨이) | 파트너가 정산 파일을 못 받았다고 함, 접수 응답은 정상 | `filegw/relay-cert-reload-retry-expire/` |
| flagkit (플래그 SDK) | 플래그를 바꿔도 일부 기능에만 반영이 안 됨 | `flagkit/key-case-default-fallback/` |
| flowctl (워크플로 엔진) | 법인 고객 연간 거래 내역서 생성이 몇 시간째 끝나지 않고 같은 단계만 반복 | `flowctl/heartbeat-timeout-restart-loop/` |
| flowctl (워크플로 엔진) | 오래된 정기결제 구독 일부가 갑자기 '종료됨' 상태로 바뀌고 다음 결제가 없음 | `flowctl/history-event-cap-terminate/` |
| flowctl (워크플로 엔진) | 배포 이후 진행 중이던 대출 심사 일부가 며칠째 '심사 중'으로 멈춤 | `flowctl/nondeterministic-replay-after-deploy/` |
| flowctl (워크플로 엔진) | 본인인증을 마친 고객 일부의 계좌 개설이 '인증 대기'에서 넘어가지 않음 | `flowctl/signal-before-start-dropped/` |
| flowctl (워크플로 엔진) | 신규 리전 오픈 후 가맹점 심사 워크플로가 시작은 되는데 첫 단계도 진행되지 않음 | `flowctl/task-queue-name-env-prefix/` |
| fxq (환율 CLI) | 환율 조회 배치가 매일 아침 40분씩 늦게 끝나 평가 리포트 반영이 늦음 | `fxq/egress-proxy-cutover-timeout/` |
| fxq (환율 CLI) | 월말 사흘간 외화 평가 리포트에 일부 통화만 나오고 나머지 통화 칸이 비어 있음 | `fxq/monthly-quota-exhausted-dashboard-poll/` |
| fxq (환율 CLI) | 외화지갑 일일 평가손익이 회계 원장과 연휴 날짜만 차이 | `fxq/tilde-quote-requery/` |
| glsync (회계 GL 연동) | 월말 마감 날에만 회계 연동 배치가 중간에 죽고 재실행해도 같은 지점에서 죽음 | `glsync/month-end-oom-full-load/` |
| glsync (회계 GL 연동) | 새 결제수단 출시 날부터 회계 연동 오류 알림이 반복됨 | `glsync/new-account-code-unregistered/` |
| glsync (회계 GL 연동) | 월말 카드 수수료 합계가 회계 장부와 몇 원~몇십 원 차이 | `glsync/rounding-mode-half-even/` |
| glsync (회계 GL 연동) | 일 마감 대사에서 정산 원장과 회계 총계정원장 금액이 일부 날만 어긋남 | `glsync/unbalanced-journal-silent-skip/` |
| hookin (PG·외부 통지 웹훅 수신기) | PG사는 통지를 보냈다는데 일부 가상계좌 입금이 반영되지 않음 | `hookin/ack-before-persist-pod-kill/` |
| hookin (PG·외부 통지 웹훅 수신기) | PG에서 취소된 결제 일부가 우리 쪽에는 '승인'으로 남아 정산 대사 차액 | `hookin/arrival-order-status-overwrite/` |
| hookin (PG·외부 통지 웹훅 수신기) | PG 통지의 약 2%가 서명 불일치로 거절됨, PG사는 서명이 맞다고 함 | `hookin/reserialized-body-hmac-mismatch/` |
| hookin (PG·외부 통지 웹훅 수신기) | PG 통지 수신율이 하루아침에 약 70%로 떨어지고 접근 로그에 403 증가 | `hookin/vendor-ip-range-allowlist-stale/` |
| hookout (가맹점 웹훅 발송기) | 가맹점 서버가 복구되자마자 웹훅 폭주로 다시 다운 | `hookout/fixed-retry-schedule-recovery-flood/` |
| hookout (가맹점 웹훅 발송기) | 대형 가맹점이 서버를 옮긴 뒤 우리 웹훅만 연결 시간 초과 | `hookout/jvm-dns-cache-stale-ip/` |
| hookout (가맹점 웹훅 발송기) | 일부 가맹점이 결제 완료 웹훅을 못 받는다고 하는데 우리 발송 기록은 200 성공 | `hookout/redirect-301-post-becomes-get/` |
| hookout (가맹점 웹훅 발송기) | 웹훅 재시도분만 가맹점에서 401로 거절되고 재시도 큐가 계속 쌓임 | `hookout/retry-reuses-signed-timestamp/` |
| hookout (가맹점 웹훅 발송기) | 모의해킹에서 가맹점 웹훅 URL로 내부 메타데이터 주소가 등록·호출됨 | `hookout/ssrf-link-local-metadata/` |
| iamctl (사내 권한 관리 CLI) | 장애 대응용 긴급 접근 권한 일부가 기한이 지나도 회수되지 않음 | `iamctl/break-glass-untagged-grant/` |
| iamctl (사내 권한 관리 CLI) | 조직 개편 후 일부 상담원만 고객 조회 화면에서 403 | `iamctl/group-deny-precedence/` |
| iamctl (사내 권한 관리 CLI) | 분기 권한 점검에서 퇴사자 일부 계정이 아직 활성으로 남아 있음 | `iamctl/hr-delta-no-catchup/` |
| iamctl (사내 권한 관리 CLI) | 권한을 회수한 상담원 계정이 회수 후 한 시간 가까이 고객 정보를 계속 조회 | `iamctl/revoke-claims-in-token/` |
| idchk (본인확인 CLI) | 월간 가맹점주 재확인에서 일부 가맹점이 '재확인 미완료'로 남아 정산 보류 | `idchk/batch-head-twelve/` |
| idchk (본인확인 CLI) | 본인확인 벤더 월 청구서가 평소의 네 배로 나옴 | `idchk/daily-cap-overage-billing-spike/` |
| idchk (본인확인 CLI) | 월간 재확인 배치에서 일요일 새벽에 처리된 가맹점만 '재확인 미완료'로 남음 | `idchk/vendor-maintenance-window-timeout/` |
| kdeploy (쿠버네티스 배포 도구) | 수수료 설정을 바꿨는데 일부 파드만 옛 수수료로 계산 | `kdeploy/configmap-subpath-not-updated/` |
| kdeploy (쿠버네티스 배포 도구) | 일별 정산 리포트가 어느 날부터 생성되지 않고 경보도 없음 | `kdeploy/cronjob-missed-schedules-stop/` |
| kdeploy (쿠버네티스 배포 도구) | 정산 배치 파드가 힙 사용량은 낮은데 하루 두세 번 OOMKilled | `kdeploy/jvm-heap-vs-container-limit-oomkill/` |
| kdeploy (쿠버네티스 배포 도구) | 배포 중 로드밸런서에서 502가 수십 건씩 튐, 앱 로그엔 오류 없음 | `kdeploy/no-prestop-endpoint-lag-502/` |
| kdeploy (쿠버네티스 배포 도구) | 배포할 때마다 1~2분간 결제 승인 API 5xx와 지연 급증 | `kdeploy/readiness-before-cache-warmup/` |
| kdeploy (쿠버네티스 배포 도구) | 같은 버전으로 배포했는데 가맹점 정산 금액이 파드에 따라 1원씩 다름 | `kdeploy/reused-tag-ifnotpresent-mixed-image/` |
| kvcache (Redis 캐시 클러스터) | 정산 지급 대사에서 같은 가맹점에 두 번 지급된 건이 몇 건씩 나옴 | `kvcache/allkeys-lru-evicts-idempotency-keys/` |
| kvcache (Redis 캐시 클러스터) | 쿠폰 오픈 시각에 캐시 한 노드만 CPU 100%, 쿠폰 조회 타임아웃 | `kvcache/hot-key-single-shard-cpu/` |
| kvcache (Redis 캐시 클러스터) | 장바구니 재고 홀드가 만료 시각이 지나도 풀리지 않아 품절로 보임 | `kvcache/keyspace-events-lost-after-failover/` |
| kvcache (Redis 캐시 클러스터) | 캐시 노드 증설 작업 중 세션 조회 오류가 20분간 급증 | `kvcache/resharding-ask-redirect-unhandled/` |
| kycflow (고객확인 심사 워크플로) | 외국인 고객 KYC 자동 거절률이 하룻밤 새 크게 늘어남 | `kycflow/foreign-name-order/` |
| kycflow (고객확인 심사 워크플로) | 아이폰 고객 일부의 사업자 서류 심사가 '업로드 완료'에서 넘어가지 않음 | `kycflow/heic-convert-silent-stall/` |
| kycflow (고객확인 심사 워크플로) | 본인확인 벤더 월 청구 건수가 내부 집계보다 2% 남짓 많음 | `kycflow/vendor-retry-double-billing/` |
| kycflow (고객확인 심사 워크플로) | 벤더는 심사 완료 통보를 보냈다는데 고객 상태가 '심사 중'에 머묾 | `kycflow/webhook-301-post-to-get/` |
| lbctl (L7 로드밸런서) | 인증 필터 배포 직후 가맹점 API 전체가 503 | `lbctl/health-check-path-behind-auth/` |
| lbctl (L7 로드밸런서) | 한가한 시간대에 가맹점 API 호출이 간헐적으로 502 | `lbctl/idle-timeout-longer-than-upstream/` |
| lbctl (L7 로드밸런서) | 정산 지급 요청이 은행으로 두 번 나간 건이 월말에만 몇 건씩 생겨 대사 차액 | `lbctl/retry-on-timeout-duplicate-post/` |
| lbctl (L7 로드밸런서) | 배포 후에도 일부 가맹점 어드민 사용자에게 옛 화면이 30분 넘게 보임 | `lbctl/sticky-session-draining-node/` |
| ledgerctl (원장 CLI) | 환불 전표가 이중 기장돼 환불 대사에서 원장 금액이 더 많음 | `ledgerctl/ha-failover-double-batch-run/` |
| ledgerctl (원장 CLI) | 환불 배치 — 영수 번호는 받았는데 입금이 없다는 문의, 환불 대사 차액 | `ledgerctl/memo-byte-silent-hold/` |
| ledgerctl (원장 CLI) | 배치 서버 재이미징 후 첫 새벽 원장 기장 배치가 인증 오류로 전부 실패 | `ledgerctl/reimage-config-owner-root/` |
| logfwd (로그 수집) | 로그 회전 직후 수집 구간이 비어 있음 | `logfwd/copytruncate-offset-gap/` |
| mailrelay (사내 메일 발송 릴레이) | DKIM 키 교체 직후 두 시간 동안 수신 측 인증 결과가 dkim=fail | `mailrelay/dkim-selector-rotate-before-dns/` |
| mailrelay (사내 메일 발송 릴레이) | 일부 고객이 몇 달째 거래 내역 메일을 못 받았다고 함, 발송 요청은 매번 접수됨 | `mailrelay/mailbox-full-hard-bounce-suppress/` |
| mailrelay (사내 메일 발송 릴레이) | 명세서 메일이 발송 로그는 정상인데 일부 메일 서비스 고객에게만 스팸함으로 감 | `mailrelay/spf-lookup-limit-permerror/` |
| mailrelay (사내 메일 발송 릴레이) | 일부 기업 가맹점 도메인으로 보낸 정산 안내 메일이 닷새 뒤 반송으로 돌아옴 | `mailrelay/tls-floor-raise-deferred-bounce/` |
| maskd (개인정보 마스킹 프록시) | 개인정보 점검에서 상담 화면 오류 팝업에 주민등록번호 뒷자리가 그대로 노출 | `maskd/content-type-bypass/` |
| maskd (개인정보 마스킹 프록시) | 고객 거래내역 조회 API가 가끔 수 초씩 걸림, 대부분은 50ms 이내 | `maskd/regex-backtrack-latency/` |
| maskd (개인정보 마스킹 프록시) | 파트너 정산 리포트 대사에서 일부 거래가 '미매칭'으로 남음 | `maskd/txid-pattern-masked-recon/` |
| mqpub (메시지 발행) | 일부 가맹점이 정산 완료 웹훅을 못 받음 | `messaging/mqpub-batch-key-head/` |
| mqpub (메시지 발행) | 정산 완료 웹훅이 오전 내내 오지 않다가 오후에 한꺼번에 도착 | `messaging/mqpub-credential-rotation-retry-backlog/` |
| mqpub (메시지 발행) | 야간 일괄 발행 배치가 밤새 실패했는데 아침 업무 시작까지 아무도 몰랐음 | `messaging/mqpub-host-disk-full-debug-log/` |
| metricdb (시계열 메트릭 DB) | VM 복구 후 결제 서버 한 대의 지표만 대시보드에서 비어 있음 | `metricdb/out-of-order-after-vm-restore/` |
| metricdb (시계열 메트릭 DB) | 월간 거래 건수 리포트가 원장 집계와 매달 몇 % 차이 | `metricdb/rollup-avg-on-counter-monthly-report/` |
| migrun (스키마 마이그레이션 러너) | 마이그레이션 배포 중 결제 승인 테이블 쓰기가 7분간 멈춤 | `migrun/add-column-default-rewrite-lock/` |
| migrun (스키마 마이그레이션 러너) | 인덱스를 추가했는데 가맹점 거래 조회가 여전히 10초 이상 | `migrun/concurrent-index-invalid-recorded-success/` |
| migrun (스키마 마이그레이션 러너) | 환불 사유 코드가 운영 DB에만 저장되지 않음, 스테이징은 정상 | `migrun/hotfix-lower-version-skipped/` |
| mlgsettle (제휴 마일리지 정산) | 일별 마일리지 합계가 맞지 않고 다음 날 반대 부호로 상쇄 | `mlgsettle/partner-cutoff-2330-window/` |
| mtragent (메트릭 에이전트) | 신규 가맹점 대시보드가 비어 있고 경보도 없음 | `mtragent/series-cap-new-merchant-blank/` |
| nodepool (노드 오토스케일러) | 일별 정산 지급 파일에 같은 가맹점 지급 줄이 두 번 들어가 은행 대사 차액 | `nodepool/scale-down-evicts-settlement-batch/` |
| nodepool (노드 오토스케일러) | 오후에 가맹점 어드민 전체가 12분간 접속 불가 | `nodepool/spot-only-single-zone-outage/` |
| notifyhub (앱 푸시 발송 게이트웨이) | 연속 결제 알림 중 일부만 보인다는 문의, 발송 기록은 전부 성공 | `notifyhub/collapse-key-offline-last-only/` |
| notifyhub (앱 푸시 발송 게이트웨이) | 앱을 다시 설치한 고객 일부가 결제 알림 푸시를 받지 못함, 발송 결과는 성공 | `notifyhub/device-token-dedup-created-at/` |
| notifyhub (앱 푸시 발송 게이트웨이) | 전 회원 대상 공지 푸시가 다 나가는 데 40분이 걸림 | `notifyhub/http2-single-connection-stream-cap/` |
| notifyhub (앱 푸시 발송 게이트웨이) | 설정 서버 이관 직후 iOS 고객만 푸시를 하나도 받지 못함 | `notifyhub/ios-sandbox-endpoint-prod-build/` |
| ocrid (신분증 진위확인) | 일부 고객의 본인확인이 '진행 중'에서 멈춤 | `ocrid/callback-underscore-header-drop/` |
| oidcbr (소셜 로그인 브로커) | 애플 로그인 고객 일부가 기존 포인트가 없는 새 계정으로 로그인됨 | `oidcbr/apple-relay-email-dup-account/` |
| oidcbr (소셜 로그인 브로커) | 통합 로그아웃 후에도 제휴몰 탭은 로그인 상태로 남음 | `oidcbr/frontchannel-logout-3p-cookie/` |
| oidcbr (소셜 로그인 브로커) | 새벽부터 한 소셜 로그인이 전부 실패, 오후에 저절로 복구 | `oidcbr/jwks-unknown-kid-no-refetch/` |
| oidcbr (소셜 로그인 브로커) | 특정 소셜 로그인 후 다시 로그인 화면으로 돌아옴, 사파리와 최신 크롬에서 | `oidcbr/samesite-form-post-state/` |
| ordhub (주문 허브) | 같은 결제 한 번에 주문이 두 건 생성돼 출고가 두 번 나감 | `ordhub/callback-idempotency-ttl-double-order/` |
| ordhub (주문 허브) | 배포 직후 주문 API 응답이 점심 피크에 8초까지 늘어남 | `ordhub/deploy-pool-size-default-p99/` |
| ordhub (주문 허브) | 라이브 방송 중 주문 확인 알림이 25분 늦게 도착하고 재고 차감도 늦음 | `ordhub/live-commerce-hot-partition-lag/` |
| ordhub (주문 허브) | 오픈마켓 주문 일부가 주문 허브에 들어오지 않았는데 마켓 쪽에는 주문이 있음 | `ordhub/webhook-secret-rotation-signature-reject/` |
| otpsvc (인증번호·OTP 서버) | 모의해킹에서 인증번호 오입력 잠금이 한도보다 훨씬 늦게 걸린다는 지적 | `otpsvc/lock-counter-per-node/` |
| otpsvc (인증번호·OTP 서버) | 저녁 7시 무렵 인증번호 문자가 5~10분 늦게 와서 입력 시간 초과 | `otpsvc/otp-shared-bulk-queue/` |
| otpsvc (인증번호·OTP 서버) | MFA 인증이 실패로 뜬 뒤 다음 코드로는 됨, 로그에는 첫 시도에도 성공 기록 | `otpsvc/totp-replay-retry-fail/` |
| outboxd (아웃박스 이벤트 릴레이) | 연말 거래 증가 후 이벤트 발행 지연이 15분까지 늘고 DB CPU 상승 | `outboxd/dead-tuple-bloat-poll-slow/` |
| outboxd (아웃박스 이벤트 릴레이) | 결제는 완료됐는데 일부 결제 이벤트가 하류 시스템에 영영 도착하지 않음 | `outboxd/sequence-commit-order-skip/` |
| payoutd (가맹점 지급 대행) | 내부 감사에서 수동 지급 건 일부가 결재자 없이 실행된 것으로 보임 | `payoutd/approver-equals-requester/` |
| payoutd (가맹점 지급 대행) | 연말 대형 가맹점 지급이 일부만 들어오고 나머지는 하루 뒤에 들어옴 | `payoutd/daily-limit-remainder-queue/` |
| payoutd (가맹점 지급 대행) | 지급 배치 로그에 같은 지급 건 처리가 두 번씩 찍히고 은행 중복 거절 응답이 다수 | `payoutd/double-instance-no-leader/` |
| payoutd (가맹점 지급 대행) | 지급 실행은 성공으로 찍혔는데 일부 법인 가맹점이 대금을 못 받았다고 함 | `payoutd/holder-name-corp-prefix-hold/` |
| pgroute (PG 라우팅 게이트웨이) | 환불 요청은 접수됐는데 일부 고객에게 환불이 며칠째 안 들어옴 | `pgroute/cancel-routed-to-other-pg/` |
| pgroute (PG 라우팅 게이트웨이) | PG 한 곳 장애 때 우회가 안 되고 그 PG로 계속 요청이 감 | `pgroute/circuit-breaker-config-key-rename/` |
| pgroute (PG 라우팅 게이트웨이) | 해외 결제 일부 통화만 PG 정산 대사 금액이 크게 어긋남 | `pgroute/currency-minor-unit-jpy/` |
| pgroute (PG 라우팅 게이트웨이) | 할인 행사 시작 직후 결제 타임아웃이 폭증하고 PG 쪽은 정상이라고 함 | `pgroute/hikari-pool-exhaust-pg-latency/` |
| pgsim (PG 환불) | 정기배송 해지 환불이 고객 계좌에 이틀 늦게 들어갔다는 민원 | `pgsim/merchant-api-key-expiry-refund-delay/` |
| pgsim (PG 환불) | 정기배송 해지·합배송 환불 후 PG 정산 대사에서 환불액 부족 | `pgsim/refund-batch-same-line/` |
| pgsim (PG 환불) | 환불 배치 응답은 정상인데 고객에게 환불이 들어가지 않음 | `pgsim/upgrade-config-template-test-endpoint/` |
| prodsrch (상품 검색) | 전체 재색인 작업 중 상품 검색 응답이 3초 이상으로 느려짐 | `prodsrch/bulk-index-refresh-interval-cpu/` |
| prodsrch (상품 검색) | 판매자가 등록한 신규 상품이 몇 시간째 검색에 나오지 않음 | `prodsrch/consumer-rebalance-loop-new-item-delay/` |
| prodsrch (상품 검색) | 사전 갱신 다음 날부터 '무선이어폰' 같은 일부 검색어 결과가 0건 | `prodsrch/user-dictionary-update-no-reindex/` |
| ptsctl (포인트 CLI) | 캐시백→포인트 이관 후 일부 회원 포인트 누락 문의 | `ptsctl/earn-member-leading-zero/` |
| ptsctl (포인트 CLI) | 인프라 작업 다음 날 아침부터 포인트 적립이 전부 '접근 거부'로 실패 | `ptsctl/nat-gateway-egress-ip-allowlist/` |
| ptsctl (포인트 CLI) | 캐시백→포인트 이관 후 일부 회원이 포인트가 안 들어왔다고 문의 | `ptsctl/vendor-outage-window-batch-no-retry/` |
| pushq (푸시 예약) | 쿠폰 만료 알림이 iOS 고객에게만 사흘째 오지 않음 | `pushq/apns-auth-cert-expiry-ios-only/` |
| pushq (푸시 예약) | 블랙프라이데이 당일 같은 푸시가 두 번씩 왔다는 고객 문의 | `pushq/run-cron-overlap-duplicate-send/` |
| pushq (푸시 예약) | 쿠폰 만료 1시간 전 알림이 거의 오지 않음 | `pushq/schedule-offslot-silent-ticket/` |
| pwreset (비밀번호 재설정 서비스) | 재설정 메일 링크를 처음 눌러도 '만료된 링크'로 나옴, 법인 고객 위주 | `pwreset/mail-scanner-consumes-token/` |
| pwreset (비밀번호 재설정 서비스) | 비밀번호를 바꾼 뒤에도 다른 기기 로그인이 유지된다는 계정 도용 민원 | `pwreset/reset-keeps-refresh-tokens/` |
| recoeng (상품 추천) | 이미 구매한 상품이 며칠째 추천 영역에 계속 나옴 | `recoeng/feature-store-lag-purchased-item-reco/` |
| recoeng (상품 추천) | 새 추천 모델 배포 뒤 같은 고객에게 새 모델·옛 모델 추천이 번갈아 나옴 | `recoeng/model-rollout-cache-key-no-version/` |
| recoeng (상품 추천) | 품절된 상품이 추천 영역에 계속 떠서 눌러 보면 품절이라는 불만 | `recoeng/out-of-stock-filter-daily-batch/` |
| reconsvc (카드 거래 대사) | 카드사 대사에서 일부 거래가 엉뚱한 날짜 거래와 짝지어져 차액이 생김 | `reconsvc/approval-no-wraparound-mismatch/` |
| reconsvc (카드 거래 대사) | 대사 리포트를 돌리는 시간에 다른 서비스 조회 화면이 옛 데이터를 보여 줌 | `reconsvc/report-full-scan-replica-lag/` |
| remitx (해외송금) | 개인정보 점검에서 송금 서버 애플리케이션 로그에 수취인 계좌번호 원문 발견 | `remitx/account-number-plaintext-log/` |
| remitx (해외송금) | 해외송금 요청이 서버 몇 대에서만 TLS 오류로 실패 | `remitx/mtls-intermediate-ca-missing/` |
| remitx (해외송금) | 해외송금이 'SENT'로 찍혔는데 수취인이 일주일째 못 받았다고 함 | `remitx/sanctions-screening-hold/` |
| remitx (해외송금) | 해외 셀러 정산 송금이 일부 금액만 도착했다는 문의 | `remitx/sha-charge-intermediary-deduct/` |
| replwatch (DB 복제 관리) | 장애 조치 뒤 30분간 들어온 주문 일부가 DB에서 사라짐 | `replwatch/failover-old-primary-still-writable/` |
| replwatch (DB 복제 관리) | 새벽에 주 DB 디스크가 가득 차 결제 승인이 전부 실패 | `replwatch/inactive-slot-wal-disk-full/` |
| replwatch (DB 복제 관리) | 가맹점 어드민에서 정산 계좌를 바꿨는데 몇 분간 옛 계좌가 보임 | `replwatch/replica-apply-paused-by-report-query/` |
| sagaorc (주문 사가 오케스트레이터) | 한정판 판매 오픈 10분 동안 주문 타임아웃이 40%까지 오름 | `sagaorc/lock-order-inversion-deadlock/` |
| sagaorc (주문 사가 오케스트레이터) | 결제 실패로 취소된 주문 일부의 쿠폰이 복원되지 않아 쿠폰 사용 대사 차액 | `sagaorc/timeout-step-excluded-from-compensation/` |
| sanctlist (제재 명단 조회) | 제재 명단의 국내 대상자 일부가 고객 대조에서 걸리지 않음 | `sanctlist/nfd-name-unmatched/` |
| sanctlist (제재 명단 조회) | 제재 명단 갱신이 10월부터 반영되지 않음, 적재 배치는 매일 성공 | `sanctlist/version-string-compare/` |
| secbox (비밀 저장소) | 한 달에 한 번꼴로 새벽에 정산 서비스만 DB 인증 실패 | `secbox/lease-max-ttl-renew/` |
| secbox (비밀 저장소) | 대규모 롤아웃 때 일부 파드가 기동 실패로 계속 재시작 | `secbox/namespace-rate-limit-rollout/` |
| secbox (비밀 저장소) | 새 결제 서명 키 적용 후 운영에서만 서명 검증 실패, 스테이징은 정상 | `secbox/pem-env-first-line/` |
| sellerrpt (판매자 정산 리포트) | 해외 판매 판매자의 정산 리포트 합계가 실제 입금액과 판매자마다 수십 원씩 다름 | `sellerrpt/fx-rounding-per-line-vs-total/` |
| sellerrpt (판매자 정산 리포트) | 월간 판매자 정산 리포트가 PG 정산과 월말 환불분만 차이 남 | `sellerrpt/month-end-refund-approval-date-basis/` |
| sellerrpt (판매자 정산 리포트) | 판매자 일 마감 리포트에서 밤 11시 이후 거래 일부가 빠짐 | `sellerrpt/replica-lag-report-tail-missing/` |
| settlebat (가맹점 정산 배치) | 배치 이미지 교체 후 첫날 정산 파일 날짜가 하루 전 날짜로 생성됨 | `settlebat/container-timezone-utc-date-shift/` |
| settlebat (가맹점 정산 배치) | 지급 보류를 풀었는데 보류 기간 매출 일부가 정산에서 빠졌다는 가맹점 문의 | `settlebat/hold-release-skips-held-period/` |
| settlebat (가맹점 정산 배치) | 정산 배치가 중간에 'No space left on device'로 멈춤 | `settlebat/tmp-disk-full-batch-abort/` |
| sharefs (공유 파일 스토리지) | 용량은 40% 남았는데 파일 업로드가 'No space left on device'로 실패 | `sharefs/inode-exhaustion-temp-files/` |
| sharefs (공유 파일 스토리지) | 정산 결과 파일이 같은 날 두 벌 생성돼 파트너 대사에서 차액 | `sharefs/nfs-nolock-concurrent-batch/` |
| shiptrack (배송 추적) | 오후 3시 이후 배송 조회가 전부 '정보 없음'으로 나옴 | `shiptrack/carrier-api-daily-quota-polling/` |
| shiptrack (배송 추적) | 일부 주문이 배송 완료로 바뀌지 않아 자동 구매 확정과 판매자 정산이 밀림 | `shiptrack/carrier-new-status-code-unmapped/` |
| shiptrack (배송 추적) | 주문한 적 없는 상품의 배송 출발 알림을 받았다는 고객 문의 | `shiptrack/waybill-number-recycle-wrong-customer/` |
| smsgw (SMS 발송 게이트웨이) | 이벤트 당첨 안내 문자가 한 통신사 가입 고객에게만 오지 않음 | `smsgw/carrier-spam-filter-keyword/` |
| smsgw (SMS 발송 게이트웨이) | 배송 안내 문자 일부가 며칠째 '발송 중'으로 남고 재발송도 되지 않음 | `smsgw/dlr-9999-unresolved-pending/` |
| smsgw (SMS 발송 게이트웨이) | 결제 인증 문자가 접수 응답은 정상인데 고객에게 도착하지 않음 | `smsgw/sender-number-dereg-dlr-3008/` |
| smsgw (SMS 발송 게이트웨이) | 대량 발송 중 게이트웨이 429 응답이 줄지 않고 발송 완료가 2시간 늦어짐 | `smsgw/tps-429-retry-no-jitter/` |
| splitab (A/B 테스트) | 50:50으로 나눈 A/B 테스트 집계 인원이 한쪽으로 4%p 기울어 판독이 멈춤 | `splitab/bot-filter-arm-asymmetry-srm/` |
| splitab (A/B 테스트) | 동시에 돌린 두 A/B 테스트가 둘 다 같은 방향으로 크게 나와 재현이 안 됨 | `splitab/hash-salt-reuse-correlated-arms/` |
| splitab (A/B 테스트) | A/B 테스트 결과가 종료 직후와 다음 날 판독에서 다르게 나옴 | `splitab/late-event-early-readout-underestimate/` |
| srchidx (검색엔진 색인 파이프라인) | 재색인 이후 새로 가입한 가맹점만 어드민 검색에 안 나옴 | `srchidx/alias-swap-writer-pinned-old-index/` |
| srchidx (검색엔진 색인 파이프라인) | 거래 검색에서 일부 거래만 빠지고 색인 배치는 매번 성공 | `srchidx/bulk-200-partial-errors-ignored/` |
| srchidx (검색엔진 색인 파이프라인) | 어드민 거래 내역 엑셀 내보내기가 돌 때마다 검색 클러스터 전체가 느려짐 | `srchidx/deep-from-size-breaker-trip/` |
| srchidx (검색엔진 색인 파이프라인) | 새로 추가한 사업자 유형으로 가맹점을 걸러 검색하면 결과가 0건 | `srchidx/dynamic-false-new-field-unsearchable/` |
| ssogw (사내 SSO) | 로그인이 간헐적으로 실패하고 다시 하면 됨 | `ssogw/saml-clock-skew-login-loop/` |
| stockpush (재고 연동) | 주말 동안 오픈마켓 재고가 갱신되지 않아 품절 상품 주문이 들어옴 | `stockpush/openmall-token-refresh-cron-missing/` |
| stockpush (재고 연동) | 재고 연동 CLI 업그레이드 직후 실행 자체가 안 되고 재고 연동이 3시간 멈춤 | `stockpush/upgrade-glibc-requirement-rollback/` |
| stockpush (재고 연동) | 창고 이전 때 판매를 멈췄는데 주문이 계속 들어옴 | `stockpush/zero-qty-noop/` |
| tickd (분산 작업 스케줄러) | 월요일에만 나가야 할 주간 리포트가 매월 1일에도 나감 | `tickd/cron-dom-dow-or-semantics/` |
| tickd (분산 작업 스케줄러) | 이자 지급 안내가 일부 고객에게 두 번 발송됨 | `tickd/leader-lease-gc-pause-double-run/` |
| tickd (분산 작업 스케줄러) | 매시 매출 집계 대시보드에 몇몇 시간대만 비고 다음 날 정산 대사 차액 | `tickd/overlap-forbid-silent-skip/` |
| tickd (분산 작업 스케줄러) | DB 점검이 끝나고 작업을 재개했는데 점검 시간대 환불 재시도 건만 처리되지 않음 | `tickd/pause-resume-window-not-backfilled/` |
| tlsrenew (인증서 자동 갱신) | 가맹점 결제창 도메인 인증서가 만료돼 결제창 접속 불가 | `tlsrenew/dns01-cname-stale-renew-fail/` |
| tlsrenew (인증서 자동 갱신) | 인증서 갱신 다음 날부터 일부 파트너사만 우리 API 호출이 TLS 오류 | `tlsrenew/missing-intermediate-java-partners/` |
| tokensvc (토큰 발급 서버) | 파트너 API 일부 엔드포인트만 간헐적 403, 재시작하면 증상이 바뀜 | `tokensvc/client-token-cache-no-scope/` |
| tokensvc (토큰 발급 서버) | 보안 점검에서 파트너 토큰이 만료 시각이 지나도 통과된다는 지적 | `tokensvc/exp-millis-never-expire/` |
| tokensvc (토큰 발급 서버) | 앱을 오래 두었다 열면 간헐적으로 강제 로그아웃 | `tokensvc/refresh-rotation-race/` |
| tokvault (카드 토큰 보관) | 결제 피크 시간에 카드 등록 응답이 2초 이상으로 느려짐 | `tokvault/hsm-session-cap-latency/` |
| tokvault (카드 토큰 보관) | 오래된 정기결제 회원만 결제 시 '토큰 복호화 실패' 오류 | `tokvault/key-rotation-old-key-disabled-early/` |
| tokvault (카드 토큰 보관) | 결제 직후 바로 취소하면 가끔 '토큰 없음'으로 실패하고 몇 초 뒤 재시도하면 됨 | `tokvault/replica-lag-token-not-found/` |
| tplsvc (메시지 템플릿 서비스) | 수수료 안내 문구를 고쳤는데 하루 동안 일부 문자만 옛 문구로 나감 | `tplsvc/cache-evict-competing-consumer/` |
| tplsvc (메시지 템플릿 서비스) | 영어로 앱을 쓰는 고객 일부가 받은 결제 안내 문자에 금액 부분이 빈칸 | `tplsvc/locale-missing-key-empty-string/` |
| tracepipe (분산 트레이싱 수집기) | 환불 처리 트레이스가 비동기 단계부터 끊겨 별도 트레이스로 흩어짐 | `tracepipe/async-executor-context-lost/` |
| tracepipe (분산 트레이싱 수집기) | 트레이스 백엔드 장애 뒤 한 시간치 스팬이 일부만 들어와 있음, 수집기는 정상 응답 | `tracepipe/exporter-queue-full-silent-drop/` |
| tracepipe (분산 트레이싱 수집기) | 결제 오류 구간의 트레이스를 찾으면 일부만 있고 대부분 비어 있음 | `tracepipe/head-sampling-drops-error-traces/` |
| unsubreg (광고 수신거부 관리) | 광고 수신을 거부한 고객 일부가 광고 알림톡을 계속 받았다는 민원 | `unsubreg/channel-scope-sms-only-sync/` |
| unsubreg (광고 수신거부 관리) | 광고 수신동의 재확인 안내 대상이 수천 명 빠지거나 엉뚱한 고객이 포함됨 | `unsubreg/consent-date-string-compare/` |
| vacct (가상계좌 발급·입금 통지) | 특정 은행 가상계좌만 발급이 안 되고 다른 은행은 됨 | `vacct/account-pool-exhausted/` |
| vacct (가상계좌 발급·입금 통지) | 행사 첫 10분 동안 가상계좌 결제 선택 고객 다수가 오류 화면을 봄 | `vacct/issue-rate-limit-429/` |
| vacct (가상계좌 발급·입금 통지) | 입금 통지가 일부만 30분~1시간 늦게 들어옴 | `vacct/notify-new-source-ip-blocked/` |
| vacct (가상계좌 발급·입금 통지) | 고객은 가상계좌로 입금했다는데 주문이 입금 대기에서 안 넘어감 | `vacct/strict-amount-mismatch-park/` |
| vanrelay (VAN 중계) | 부분취소 건의 부가세 대사만 어긋남 | `vanrelay/partial-cancel-vat-zero/` |
| wafedge (웹 방화벽) | WAF 규칙 갱신 다음 날 정산 배치의 내부 API 호출만 403 | `wafedge/empty-ua-managed-rule/` |
| wafedge (웹 방화벽) | 대형 가맹점 한 곳만 점심 피크에 결제 API가 429로 실패 | `wafedge/nat-rate-limit/` |
| wafedge (웹 방화벽) | 카드사가 보냈다는 정산 결과 통보 일부가 우리 쪽에 도착하지 않음 | `wafedge/partner-callback-traversal-rule/` |
| wafedge (웹 방화벽) | 간편송금 제휴사 결과 통보의 절반가량이 도착하지 않음, 재전송하면 들어오기도 함 | `wafedge/partner-ip-allowlist-single/` |
| wmsbridge (창고 WMS 연동) | 입고한 상품이 하루 만에 품절로 표시되는데 창고에는 재고가 잔뜩 있음 | `wmsbridge/asn-case-unit-inventory-understated/` |
| wmsbridge (창고 WMS 연동) | 출고는 됐는데 일부 주문만 출고 확정이 반영되지 않아 고객 배송 조회가 비어 있음 | `wmsbridge/confirm-file-read-while-writing/` |
| wmsbridge (창고 WMS 연동) | 창고 로케이션 개편 첫날 피커들이 상품을 못 찾아 출고가 반나절 지연 | `wmsbridge/location-code-migration-picklist-old/` |
| wmsbridge (창고 WMS 연동) | 당일 출고 약속 주문 일부가 다음 날 출고돼 배송 지연 보상 요청 | `wmsbridge/wave-cutoff-queue-lag-next-day-ship/` |
| xferq (파일 전송 CLI) | 카드사에 다시 보낸 대사 파일을 카드사가 못 받았다고 함 | `xferq/name-underscore-silent-park/` |
| xferq (파일 전송 CLI) | 파일 전송 배치가 '토큰 유효 시간' 오류로 매시간 실패 | `xferq/ntp-stopped-token-not-yet-valid/` |
| xferq (파일 전송 CLI) | 카드사가 대사 파일을 사흘째 못 받았다고 함, 송신 배치는 재시도 중 | `xferq/vendor-dc-move-hostkey-mismatch/` |
