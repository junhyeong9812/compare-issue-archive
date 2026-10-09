# issue/dnsops/ndots-search-domain-amplification — 정리 (힌트)

태그: `performance` · `dns`

## 전체 흐름

```
api.pgx.com (점 2 < ndots 5)
 → api.pgx.com.pay.svc.cluster.local   NXDOMAIN
 → api.pgx.com.svc.cluster.local       NXDOMAIN
 → api.pgx.com.cluster.local           NXDOMAIN
 → api.pgx.com.hanbit.internal         NXDOMAIN
 → api.pgx.com.                        OK
A·AAAA 각각 → 질의 10회
```

## 핵심 문장

- `ndots` 미만의 점을 가진 이름은 **검색 도메인을 먼저 붙여** 본다 — 외부 이름 하나가 질의 여러 번이 된다.
- A·AAAA를 따로 물으면 두 배가 된다.
- 피크에 커넥션 풀이 새 커넥션을 열 때마다 이 비용이 붙는다.
- 외부 이름은 끝에 `.`(FQDN)을 붙이거나 파드 `dnsConfig`로 `ndots:2`를 준다.
