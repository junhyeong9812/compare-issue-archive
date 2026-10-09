# issue/hookout/jvm-dns-cache-stale-ip — 정리 (힌트)

태그: `infra` · `caching`

## 전체 흐름

```
가맹점 DNS 변경 (TTL 300s) → 다른 서비스들은 5분 뒤 새 IP
hookout JVM: networkaddress.cache.ttl=-1 (보안 정책 파일에 설정) → 기동 시 IP 영구 캐시
옛 IP 서버 철거 → connect timeout → 재시도 → 실패 누적
재기동 후 정상
```

## 핵심 문장

- JVM은 보안 관리자 정책에 따라 DNS 성공 결과를 **영구 캐시**할 수 있다 — 이 이미지는 `-1`(무기한)이 설정돼 있었다.
- 레코드의 TTL을 무시하므로 상대가 IP를 바꾸면 재기동 전까지 따라가지 못한다.
- 대처: `networkaddress.cache.ttl=60`, 연결 실패 시 해당 호스트 캐시를 비우고 재조회하는 래퍼.
