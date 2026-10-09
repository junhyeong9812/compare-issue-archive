# issue/egresspx/ip-allowlist-partner-ip-changed — 정리 (힌트)

태그: `silent-failure` · `network`

## 전체 흐름

```
통지 서비스 → egresspx (allow: 203.0.113.10) → 카드사 새 IP 198.51.100.7
egresspx: 403 (deny) → 통지 서비스 재시도 큐 (최대 7일)
접수 응답은 정상, 카드사 미수신
```

## 핵심 문장

- IP 허용은 **상대가 IP를 바꾸면 조용히 깨진다**.
- 프록시 거부(403)를 일반 일시 오류로 보고 재시도했다 — 재시도로 안 풀리는 오류다.
- 도메인(SNI) 기반 허용은 IP 변경을 따라간다.
- 프록시 거부는 재시도하지 말고 즉시 경보로 올린다.
