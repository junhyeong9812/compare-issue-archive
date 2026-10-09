# issue/tracepipe/async-executor-context-lost — 정리 (힌트)

태그: `observability` · `context`

## 전체 흐름

```
요청 스레드: span(refund-api) ── supplyAsync(task, refundPool)
refundPool 스레드: 컨텍스트 없음 → span(pg-call) 새 루트
@Async: 에이전트가 계측한 스프링 실행기 → 컨텍스트 전파
```

## 핵심 문장

- 트레이스 컨텍스트는 **스레드 로컬**이라 다른 스레드로 넘길 때 감싸야 한다.
- 에이전트는 알려진 실행기만 자동 계측한다 — 직접 만든 풀은 빠질 수 있다.
- MDC도 스레드 로컬이라 같이 끊긴다.
- 실행기를 `Context.taskWrapping(...)`으로 감싸면 둘 다 해결된다.
