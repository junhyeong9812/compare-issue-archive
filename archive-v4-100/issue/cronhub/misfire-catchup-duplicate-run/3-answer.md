# issue/cronhub/misfire-catchup-duplicate-run — 정답

태그: `race-condition` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **세 번(02:00·03:00·04:00 예정분) 연달아 실행된다.** 기본 misfire 정책이 `fire_all`이기 때문이다.\
세 실행 모두 실제 시각은 04:20 언저리다. 예정 시각은 작업 컨텍스트의 `scheduled_at`으로만 알 수 있다.
   > **misfire** — 스케줄러가 예정 시각에 작업을 시작하지 못한 상황. 재기동 후 그 회차를 어떻게 처리할지가 misfire 정책이다.

2. **작업이 대상 기간을 실행 시각으로 정했기 때문이다.**\
리포트 작업은 `today()`로 "어제 하루"를 계산했다. 세 실행 모두 04:20에 돌았으니 같은 날짜를 보고 같은 리포트를 만들었다.\
발송 이력 테이블도 없어 세 번 모두 보냈다.

3. **은행이 같은 파일 일련번호를 거절해 줬다.** 사내 작업은 파일을 세 번 만들어 세 번 전송했다.\
이 방어는 은행 쪽 규칙이고 사내가 통제하지 못한다 — 파일 일련번호 부여 방식이 바뀌면 이중 지급이 된다. 기대면 안 된다.

4. **`fire_all`**: 놓친 회차마다 처리할 고유 구간이 있고(예: 시간별 집계), 작업이 예정 시각을 기준으로 동작할 때.\
**`fire_once`**: 최신 상태 한 번이면 충분한 작업(리포트·캐시 갱신).\
**`skip`**: 시각이 지나면 의미가 없는 작업(정시 알림·장 마감 직전 처리).

5. **같은 대상을 여러 번 처리하거나, 놓친 대상을 영영 처리하지 않는다.**\
몰아 실행이면 중복이 되고, 자정을 넘겨 재기동하면 "어제"가 바뀌어 원래 처리했어야 할 날짜가 빠진다.\
대상은 예정 시각에서 유도해야 재실행·지연 실행에서 결정적이다.

6. **멱등성 — 같은 (작업, 대상 기간)을 두 번 처리해도 결과가 한 번과 같아야 한다.**\
결과물에 멱등 키를 두고(발송 이력·파일 일련번호의 사내 유일 제약), 이미 있으면 건너뛴다.\
스케줄러 정책은 비용(쓸데없는 실행)을 줄이는 장치고, 정확성은 작업이 책임진다.

## 문제 구조 (추상화 코드)

### 변형 A — 실행 시각으로 대상 결정
① 문제 코드
```java
@Job(cron = "0 0 * * * *")
void dailyReport() {
    LocalDate target = LocalDate.now().minusDays(1);      // 몰아 실행 3회 모두 같은 target
    mailer.sendAll(reportFor(target));
}
```
② 고친 코드
```java
@Job(cron = "0 0 * * * *", misfire = FIRE_ONCE)
void dailyReport(JobContext ctx) {
    LocalDate target = ctx.scheduledAt().toLocalDate().minusDays(1);
    if (!sentLog.tryClaim("daily-report", target)) return;   // (작업, 대상) 유일 제약
    mailer.sendAll(reportFor(target));
}
```
무엇이 깨졌나: 작업이 "한 번만 정시에 돈다"고 가정했다 — 스케줄러는 그 가정을 지켜 주지 않았다.

### 변형 B — 하류 중복 거절에 기댄 지급 파일
① 문제 코드
```java
Path f = payoutFile.build(LocalDate.now());
bank.upload(f);                                          // 중복은 은행이 거절해 주길 기대
```
② 고친 코드
```java
LocalDate d = ctx.scheduledAt().toLocalDate();
if (!payoutLog.tryClaim(d)) return;
bank.upload(payoutFile.build(d));
```
무엇이 깨졌나: 돈이 움직이는 작업의 중복 방어를 외부 시스템 규칙에 맡겼다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
