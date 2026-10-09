# issue/kdeploy/cronjob-missed-schedules-stop — 정리 (힌트)

태그: `silent-failure` · `scheduler`

## 전체 흐름

```
컨트롤러 정지 18시간 → 10분 주기 = 놓친 실행 108회
재개 시 lastScheduleTime 이후 놓친 횟수 > 100
  → "Cannot determine if job needs to be started. Too many missed start times" (이벤트만)
  → 이후 실행 없음
하루 1회 CronJob: 놓친 1회 → 정상 재개
```

## 핵심 문장

- `startingDeadlineSeconds`가 없으면 마지막 실행 이후 **놓친 횟수를 전부 센다** — 100회를 넘으면 시작하지 않는다.
- 이 상태는 **이벤트로만** 남고 CronJob 상태는 정상처럼 보인다.
- `startingDeadlineSeconds`를 두면 그 창 안의 놓친 횟수만 센다.
- 리포트는 '마지막 성공 시각' 경보로 감시한다.
