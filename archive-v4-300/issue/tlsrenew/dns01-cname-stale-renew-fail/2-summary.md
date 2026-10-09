# issue/tlsrenew/dns01-cname-stale-renew-fail — 정리 (힌트)

태그: `tls` · `silent-failure`

## 전체 흐름

```
_acme-challenge.pay CNAME → pay.acme-old.hanbit.internal (옛 존, 갱신 안 됨)
tlsrenew: TXT 를 새 존에 기록 → CA 는 옛 존 조회 → 검증 실패
실패 로그: 작업 로그에만 (경보 없음) → 30일 후 만료
```

## 핵심 문장

- DNS-01은 **CA가 실제로 조회하는 이름**에 TXT가 있어야 한다 — CNAME 위임 대상이 바뀌면 깨진다.
- 갱신 실패 알림이 작업 로그에만 있었다.
- 인증서 만료 경보는 7일 전이었고 주말에 울려 늦었다.
- 갱신 실패 즉시 경보 + 만료 21일 전 경보 + 존 이전 체크리스트에 검증 위임 확인.
