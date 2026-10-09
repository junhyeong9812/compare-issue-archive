# issue/dnsops/negative-cache-before-record-created — 정리 (힌트)

태그: `stale-read` · `dns`

## 전체 흐름

```
t0     배치 서버 3대 조회 → NXDOMAIN → 캐시 리졸버가 '없음' 저장 (SOA minimum 3600s)
t0+10m 레코드 생성 (TTL 60)
t0+10m~60m  3대가 쓰는 리졸버 노드: 캐시된 NXDOMAIN 응답
           다른 리졸버 노드: 캐시 없음 → 정상 조회
```

## 핵심 문장

- '없음' 응답도 캐시된다 — 기간은 레코드 TTL이 아니라 **존 SOA의 minimum(부정 응답 TTL)**이다.
- 캐시는 **리졸버 노드별**이라 사전 조회를 한 서버가 붙은 노드만 실패한다.
- 새 레코드 TTL은 '없음' 캐시에 영향을 주지 않는다.
- 오픈 전 사전 조회를 금지하거나, 레코드 생성 후 `dnsops flush <name>`으로 리졸버 캐시를 비운다.
