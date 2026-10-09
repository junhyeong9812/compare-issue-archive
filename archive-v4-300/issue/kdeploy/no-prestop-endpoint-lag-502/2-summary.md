# issue/kdeploy/no-prestop-endpoint-lag-502 — 정리 (힌트)

태그: `deploy` · `race`

## 전체 흐름

```
t0  파드 Terminating ─┬─ SIGTERM → 앱 리스너 닫힘 (t0+0.1s)
                      └─ 엔드포인트 제거 → LB 반영 (t0+2~5s)
t0+0.1~5s  LB 가 아직 이 파드로 보냄 → connection refused → 502
```

## 핵심 문장

- SIGTERM과 엔드포인트 제거는 **동시에 비동기로** 일어난다 — 순서 보장이 없다.
- 닫힌 소켓으로 간 요청은 앱에 닿지 않으니 앱 로그엔 흔적이 없고 LB 로그에만 남는다.
- graceful shutdown은 **이미 받은** 요청만 지킨다 — 새로 오는 요청은 못 막는다.
- `preStop`에 몇 초 대기를 넣어 엔드포인트 제거가 퍼진 뒤 종료가 시작되게 한다.
