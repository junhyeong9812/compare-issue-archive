# issue/kdeploy/cronjob-missed-schedules-stop — 정답

태그: `silent-failure` · `scheduler`

## 정답
<!-- 질문 1:1 대응 -->

1. **스케줄이 멈춘다.** 놓친 실행이 108회라 컨트롤러가 다음 실행을 만들지 않았다.

2. **주기가 달라서다.** 하루 1회는 18시간 동안 1회만 놓쳤다.

3. **최근 600초 안의 놓친 횟수(1회)만 세므로 정상 재개된다.**

4. **`last_success_timestamp` 경과 26시간 초과 경보.** 리포트 파일 존재 여부를 따로 확인한다.

## 문제 구조 (추상화 코드)

### 변형 A — 짧은 주기 CronJob에 deadline 없음
① 문제 코드
```yaml
kind: CronJob
spec:
  schedule: "*/10 * * * *"
  concurrencyPolicy: Forbid
```
② 고친 코드
```yaml
kind: CronJob
spec:
  schedule: "*/10 * * * *"
  concurrencyPolicy: Forbid
  startingDeadlineSeconds: 600
```
무엇이 깨졌나: 스케줄러가 멈춘 뒤 스스로 회복한다고 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| startingDeadlineSeconds 지정 | 템플릿 수정 | 작음 | 창 밖 실행 누락 | 기본 |
| 마지막 성공 시각 경보 | 메트릭 푸시 | 작음 | 없음 | 함께 |
| CronJob 재생성 | 수동 | 작음 | 원인 미해결 | 복구 시 |

**결론**: 템플릿 기본값에 deadline을 넣고, 결과물 기준 경보를 둔다. 빠진 사흘치는 수동 실행했다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
