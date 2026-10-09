# issue/egresspx/snat-port-exhaustion-peak — 정리 (힌트)

태그: `performance` · `network`

## 전체 흐름

```
출발 IP 2개 × 사용 가능 포트 약 28,000 = 56,000
요청마다 새 연결 → 연결당 TIME_WAIT 60초 점유
초당 약 930건 이상 → 포트 고갈 → SYN 못 보냄 → 타임아웃
```

## 핵심 문장

- 같은 목적지(IP·포트)로 가는 연결은 **출발 IP×포트 조합**이 한정된다.
- `Connection: close` 응답을 주는 PG 때문에 풀이 재사용을 못 했다 — 응답 헤더를 확인했어야 했다.
- 출발 IP를 늘리면 상한은 오르지만 근본은 연결 재사용이다.
- PG에 keep-alive 협의, 그전까지 출발 IP 증설과 포트 사용률 경보.
