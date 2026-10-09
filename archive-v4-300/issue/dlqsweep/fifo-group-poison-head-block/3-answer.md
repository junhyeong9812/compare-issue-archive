# issue/dlqsweep/fifo-group-poison-head-block — 정답

태그: `ordering` · `availability`

## 정답
<!-- 질문 1:1 대응 -->

1. **전달되지 않는다.** 그룹 순서를 지키기 위해 m1 뒤에서 대기한다.

2. **다른 그룹들이 정상 처리돼 평균 지연이 낮았다.** 그룹별 최고 나이를 보지 않았다.

3. **그 메시지만 순서에서 빠진다.** 정산 알림은 독립적이라 허용했지만, 순서가 업무상 필수인 큐는 사람 확인 후 진행한다.

4. **지연된 대상들이 같은 메시지 그룹인지, 그 그룹 맨 앞 메시지의 수신 횟수를 본다.**

## 문제 구조 (추상화 코드)

### 변형 A — DLQ 없는 순서 보장 큐
① 문제 코드
```yaml
queue: settle-notify.fifo
redrive_policy: null
```
② 고친 코드
```yaml
queue: settle-notify.fifo
redrive_policy: { dead_letter: settle-notify-dlq.fifo, max_receive: 5 }
alerts: { oldest_message_age_per_group: "> 10m" }
```
무엇이 깨졌나: 순서 보장과 독 메시지 격리가 함께 설계되지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| DLQ + 최대 수신 | DLQ 처리 절차 | 작음 | 순서 빠짐 | 기본 |
| 생산자 스키마 검사 | 공용 스키마 | 중간 | 구버전 잔존 | 함께 적용 |

**결론**: 순서 보장 큐에는 반드시 DLQ를 두고, 그룹별 최고 대기 시간을 감시한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
