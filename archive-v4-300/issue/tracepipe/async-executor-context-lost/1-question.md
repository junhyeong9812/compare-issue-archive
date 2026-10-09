# issue/tracepipe/async-executor-context-lost — 스레드 풀로 넘긴 작업은 트레이스 컨텍스트를 자동으로 가져가지 않는다 — 질문

## 질문
1. (예측) 환불 API가 `CompletableFuture.supplyAsync(task, refundPool)`로 PG 호출을 넘긴다. `refundPool`은 직접 만든 `ThreadPoolExecutor`다. PG 호출 스팬의 부모는?
2. (왜) 다른 비동기 코드(@Async)는 이어졌다. 왜?
3. (경계) MDC 로그 상관 ID도 끊기는가?
4. (연결) 고치는 위치는?

## 복습 기록
| 날짜 | 결과 | 틀린 질문 | 다음 복습 |
|------|------|-----------|-----------|
| | | | |
