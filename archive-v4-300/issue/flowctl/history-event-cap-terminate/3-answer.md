# issue/flowctl/history-event-cap-terminate — 정답

태그: `limits` · `long-running`

## 정답
<!-- 질문 1:1 대응 -->

1. **엔진이 종료시킨다.** 다음 결제 단계는 실행되지 않는다.

2. **카드 한도 부족으로 매달 재시도가 많던 고객의 이력이 빨리 쌓였다.**

3. **다음 결제일·누적 실패 횟수 같은 최소 상태만.** 이력 전체가 아니다.

## 문제 구조 (추상화 코드)

### 변형 A — 단일 실행 무한 루프
① 문제 코드
```java
while (true) { Workflow.sleep(untilNextBilling()); charge(); }
```
② 고친 코드
```java
Workflow.sleep(untilNextBilling());
charge();
Workflow.continueAsNew(new SubState(nextBillingDate(), failureCount()));
```
무엇이 깨졌나: 수명이 무한한 업무를 상한이 있는 실행 하나에 담았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 주기마다 continueAsNew | 상태 직렬화 | 작음 | 상태 누락 | 기본 |
| 이력 상한 경보 | 지표 | 작음 | 없음 | 함께 적용 |

**결론**: 장기 워크플로는 주기마다 새 실행으로 넘기고, 이력 크기를 감시한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
