# issue/flowctl/nondeterministic-replay-after-deploy — 정리 (힌트)

태그: `deploy` · `determinism`

## 전체 흐름

```
기존 코드:  신용조회 → 소득확인 → 결정
새 코드:    신용조회 → [신규] 사기점수 → 소득확인 → 결정
진행 중 실행 W1 이력: ActivityScheduled(신용조회) Completed, ActivityScheduled(소득확인) ...
워커 재기동 → W1 재생: 새 코드는 두 번째로 사기점수를 예약하려 함 → 이력과 불일치
→ NonDeterministicError → 워크플로 태스크 재시도 무한 반복 (실행 상태는 RUNNING)
```

## 핵심 문장

- 이벤트 재생형 엔진은 워커가 실행 상태를 **이력 재생으로 복원**한다 — 코드는 같은 이력에 같은 명령을 내야 한다.
- 진행 중 실행이 있는 워크플로에 단계를 끼워 넣으면 재생이 어긋난다.
- 실행 상태는 RUNNING으로 보이고 오류는 워크플로 태스크 실패 지표에만 쌓였다.
- 대처: 버전 분기(`getVersion("fraud-score", DEFAULT, 1)`)로 새 실행만 새 경로, 배포 전 이력 재생 테스트.
