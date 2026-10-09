# issue/connpx/txn-pool-session-timezone-leak — 정리 (힌트)

태그: `contract-drift` · `silent-failure`

## 전체 흐름

```
리포트 서비스 ─SET TIME ZONE 'UTC'─▶ connpx ─▶ 백엔드 커넥션 #7 (세션 상태: UTC)
승인 서버   ─INSERT ... now()::date─▶ connpx ─▶ 백엔드 커넥션 #7 (여전히 UTC)
                                                  └ 00:00~08:59 KST 거래 → 전날 날짜
connpx server_reset_query = (빈 값)   ← 트랜잭션 풀링 기본값
```

## 핵심 문장

- 트랜잭션 풀링에서는 **트랜잭션이 끝나면 백엔드 커넥션이 다른 클라이언트에게 간다** — 세션 단위 `SET`은 그대로 따라간다.
- `connpx`의 `server_reset_query`는 트랜잭션 풀링 모드에서 기본으로 실행되지 않는다 — 오류도 경고도 없다.
- 증상은 **같은 커넥션을 이어 받은 요청만** 날짜가 하루 밀리는 것이다 — 그래서 매일 건수가 다르다.
- 세션 설정은 `SET LOCAL`로, 거래일은 앱이 명시적 시간대로 계산해 넣는다.
