# issue/flowctl/signal-before-start-dropped — 정답

태그: `race` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **대상 없음 오류로 거절된다.** 나중에 시작된 워크플로는 그 신호를 받지 못한다.

2. **인증을 아주 빨리 끝낸 고객만 시작보다 신호가 앞섰다.** 큐 지연이 큰 시간대에 몰렸다.

3. **대부분 해결되지만 시작이 오래 지연되면 재시도도 소진된다.** `signalWithStart`가 순서와 무관하게 안전하다.

4. **인증 서비스 로그에서 해당 고객의 신호 결과를 본다.** WorkflowNotFound면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 실행 존재를 가정한 신호
① 문제 코드
```java
client.signal(workflowId, "verified", result);
```
② 고친 코드
```java
client.signalWithStart(workflowId, AccountOpenWorkflow::start, request,
                       "verified", result);             // 없으면 시작 + 신호, 있으면 신호
```
무엇이 깨졌나: 두 비동기 경로(시작·신호)의 도착 순서를 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| signalWithStart | 시작 입력을 신호 쪽이 앎 | 작음 | 없음 | 기본 |
| 시작 시 인증 결과 조회 | 조회 API | 작음 | 조회 지연 | 함께 적용 |

**결론**: 순서를 보장할 수 없는 신호는 signalWithStart로 보내고, 실패를 로그로 삼키지 않는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
