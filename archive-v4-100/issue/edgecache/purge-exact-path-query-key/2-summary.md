# issue/edgecache/purge-exact-path-query-key — 정리 (힌트)

태그: `contract-drift` · `single-source-of-truth`

## 전체 흐름

```
캐시 키 = host + path + query (전체 문자열)

엣지 캐시
  /terms                 ← 웹      ── purge /terms 로 삭제 ✔
  /terms?v=app5.1        ← 앱 5.1  ── 남음 ✘ (TTL 7일)
  /terms?v=app5.2        ← 앱 5.2  ── 남음 ✘

purge <path> = 키 정확 일치 1건 삭제 → "purged 1"
purge --prefix <path> = 키 접두 일치 삭제 → "purged 3"

근본 교정: 내용 해시를 URL 에 넣는다 (/terms/3f9a1c.html) → 퍼지 불필요
          또는 캐시 키에서 쿼리 무시 설정 (쿼리가 내용에 영향 없을 때)
```

## 핵심 문장

- `edgecache`의 캐시 키는 **경로+쿼리 문자열 전체**고, `purge <path>`는 **정확 일치 한 건**만 지운다.
- 앱 버전마다 다른 쿼리(`?v=...`)로 열어 버전마다 **별도 캐시 항목**이 있었다 — 퍼지는 쿼리 없는 항목만 지웠다.
- `purged N`은 지운 키 개수다 — N이 기대(버전 수)보다 적으면 남은 항목이 있다.
- 접두 퍼지(`--prefix`)로 지우거나, 쿼리를 캐시 키에서 빼거나, 내용 해시로 URL을 바꾼다.
- 확인은 퍼지 후 실제 사용 URL 각각에 요청해 응답 헤더(`Age`·캐시 상태)를 본다.
