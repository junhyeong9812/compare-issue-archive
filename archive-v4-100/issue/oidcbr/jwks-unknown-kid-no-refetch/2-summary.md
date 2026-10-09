# issue/oidcbr/jwks-unknown-kid-no-refetch — 정리 (힌트)

태그: `caching` · `availability`

## 전체 흐름

```
14:00 JWKS 캐시 {kid: A}
05:00 IdP 키 회전 → 새 토큰 kid: B
oidcbr: kid B 캐시에 없음 → 검증 실패 (재조회 없음)
14:00 캐시 갱신 → {B} → 복구
교정: 모르는 kid → 즉시 1회 재조회 (최소 간격 5분)
```

## 핵심 문장

- 캐시에 없는 `kid`가 와도 **다시 받지 않아** 다음 정기 갱신까지 전부 실패했다.
- IdP는 회전 직후 새 키로만 서명했다 — 겹치는 기간이 짧았다.
- 모르는 kid면 **즉시 1회 재조회**하되, 최소 간격을 둬 위조 kid 폭주를 막는다.
- 캐시 수명은 IdP 응답의 Cache-Control을 따른다.
