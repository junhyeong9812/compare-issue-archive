# issue/sagaorc/timeout-step-excluded-from-compensation — 정답

태그: `compensation` · `partial-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **복원되지 않는다.** 실제 차감은 됐지만 사가는 ②를 보상 대상으로 보지 않았다.

2. **대부분 고객은 바로 다시 주문하지 않아 쿠폰 소실을 몰랐다.** 월말 쿠폰 사용 수와 완료 주문 수가 1,900건 어긋났다.

3. **차감이 실제로 안 된 경우 복원이 잘못 늘어날 수 있다** — 그래서 보상 API가 주문 id 기준 멱등이어야 한다(차감 기록 없으면 무시).

4. **차액 건의 사가 단계 상태를 본다.** 쿠폰 단계가 TIMEOUT인데 쿠폰 서비스에는 차감 기록이 있으면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — COMPLETED 단계만 보상
① 문제 코드
```java
saga.steps().stream().filter(s -> s.status() == COMPLETED)
    .sorted(reverseOrder()).forEach(Step::compensate);
```
② 고친 코드
```java
saga.steps().stream().filter(s -> s.status() == COMPLETED || s.status() == TIMEOUT)
    .sorted(reverseOrder())
    .forEach(s -> s.compensate(saga.orderId()));     // 보상은 orderId 기준 멱등
```
무엇이 깨졌나: "응답이 없었다"를 "처리되지 않았다"로 해석했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| TIMEOUT 포함 + 멱등 보상 | 보상 API 멱등 | 작음 | 없음 | 기본 |
| 보상 전 상태 조회 | 조회 API | 호출 증가 | 조회도 시간 초과 | 함께 적용 |

**결론**: 시간 초과 단계는 보상 대상에 넣고, 보상은 멱등하게 만든다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
