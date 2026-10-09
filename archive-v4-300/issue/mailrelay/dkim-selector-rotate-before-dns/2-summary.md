# issue/mailrelay/dkim-selector-rotate-before-dns — 정리 (힌트)

태그: `deploy-order` · `auth`

## 전체 흐름

```
09:00  mailrelay 설정: selector=s2026 / 새 개인키로 서명 시작
09:00  DNS 등록 요청: s2026._domainkey TXT (변경 승인 대기)
10:55  DNS 반영
09:00~10:55  수신 서버: s2026 공개키 조회 → NXDOMAIN → dkim=fail
             (옛 셀렉터 s2025 레코드는 같은 작업에서 삭제됨)
```

## 핵심 문장

- 수신 서버는 서명 헤더의 셀렉터로 **DNS에서 공개키를 조회**한다 — 레코드가 없으면 실패다.
- 교체 순서가 거꾸로였다: 서명 전환을 DNS 반영보다 먼저 했고, 옛 레코드는 같은 날 지웠다.
- 올바른 순서: **새 레코드 등록 → 외부 리졸버에서 조회 확인 → 서명 전환 → 옛 레코드는 7일 뒤 삭제**.
