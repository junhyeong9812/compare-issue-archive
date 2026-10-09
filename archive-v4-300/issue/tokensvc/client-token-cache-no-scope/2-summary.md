# issue/tokensvc/client-token-cache-no-scope — 정리 (힌트)

태그: `caching` · `authorization`

## 전체 흐름

```
cache key = client_id
첫 호출 조회 → 토큰(scope=read) 캐시 → 지급 API 에 재사용 → 403
첫 호출 지급 → 토큰(scope=payout) 캐시 → 조회 API 403
재시작 → 첫 호출 종류에 따라 뒤바뀜
교정: key = client_id + 정렬된 scope
```

## 핵심 문장

- 토큰 캐시 키가 **client_id뿐**이라 먼저 받은 scope의 토큰을 다른 API에도 썼다.
- 기동 후 **첫 호출 종류**가 어느 쪽이 실패할지 정했다.
- 지급은 재시도 큐가 있어 403이 지연으로만 보였다.
- 캐시 키는 **client_id + scope 집합**으로 한다.
