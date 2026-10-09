# issue/artihub/remote-404-cached-new-release — 정리 (힌트)

태그: `stale-read` · `ci`

## 전체 흐름

```
t0     빌드 → artihub → 원격 404 → '없음' 캐시 (missed_retrieval_ttl 24h)
t0+30m 원격에 3.2.1 공개
t0+30m~24h 빌드 → artihub 캐시 404 → 실패
```

## 핵심 문장

- 원격 프록시는 **404도 캐시**한다 — 기간은 `missed_retrieval_ttl`(기본 24시간)이다.
- 개인 PC는 사내 프록시를 거치지 않아 바로 받았다.
- 버전 목록 메타데이터는 별도 TTL(`metadata_ttl`, 30분)이다.
- 긴급 시 `artihub cache zap <repo> <path>`로 해당 경로 캐시를 지운다. TTL은 30분으로 낮췄다.
