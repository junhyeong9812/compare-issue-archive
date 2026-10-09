# issue/flowctl/history-event-cap-terminate — 정리 (힌트)

태그: `limits` · `long-running`

## 전체 흐름

```
subscription workflow: while(true) { sleep(1달); charge(); notify(); ... }
매달 이벤트 약 60개 + 재시도 → 오래된 고객은 이력 5만 개 근접
엔진 상한 50,000 이벤트 → 실행 강제 종료(TERMINATED)
```

## 핵심 문장

- 엔진은 실행당 **이력 이벤트 수·크기 상한**이 있다 — 넘으면 실행을 종료한다.
- 무한 루프 워크플로는 언젠가 상한에 닿는다 — 결제 실패 재시도가 많은 고객이 먼저 닿았다.
- 대처: 매 결제 주기 끝에 `continueAsNew`로 이력을 새로 시작, 상한 80% 경보.
