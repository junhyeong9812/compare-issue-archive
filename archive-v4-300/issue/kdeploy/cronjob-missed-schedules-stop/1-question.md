# issue/kdeploy/cronjob-missed-schedules-stop — CronJob은 놓친 실행이 100회를 넘으면 스케줄을 멈춘다 — 질문

## 질문
1. (예측) 10분 주기 CronJob(`startingDeadlineSeconds` 없음)이 있다. 컨트롤 플레인 업그레이드로 컨트롤러가 18시간 멈췄다가 돌아오면 이 CronJob은?
2. (왜) 다른 CronJob(하루 1회)은 정상 재개됐다. 왜 차이가 났나?
3. (경계) `startingDeadlineSeconds: 600`이면?
4. (연결) 일별 리포트 생성 여부는 무엇으로 감시하나?

## 복습 기록
| 날짜 | 결과 | 틀린 질문 | 다음 복습 |
|------|------|-----------|-----------|
| | | | |
