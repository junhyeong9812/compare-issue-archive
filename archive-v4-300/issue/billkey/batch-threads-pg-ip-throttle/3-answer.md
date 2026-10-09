# issue/billkey/batch-threads-pg-ip-throttle — 정답

태그: `performance` · `rate-limit`

## 정답
<!-- 질문 1:1 대응 -->

1. **PG의 IP 단위 한도를 넘어 거절됐다.**

2. **정정 안내를 보내고 해당 건을 당일 재청구해 성공시켰다.**

3. **계약 위반이다.** PG와 한도 상향을 협의했다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
ExecutorService pool = Executors.newFixedThreadPool(64);
```
② 고친 코드
```java
RateLimiter limiter = RateLimiter.create(90);   // PG 한도 아래
// E-THROTTLE → requeue, 회원 알림 없음
```
무엇이 깨졌나: 외부 한도를 모른 채 병렬도를 올렸고, 한도 거절을 업무 실패로 분류했다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 속도 제한 + 재큐잉 | 작음 | 배치 시간 | 기본 |
| 한도 상향 협의 | 협의 | 비용 | 병행 |

**결론**: 병렬도는 상대 한도로 정하고, 기술적 거절은 업무 실패와 구분한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
