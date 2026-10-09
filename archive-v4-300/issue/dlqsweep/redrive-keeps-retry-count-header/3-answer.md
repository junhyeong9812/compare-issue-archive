# issue/dlqsweep/redrive-keeps-retry-count-header — 정답

태그: `retry` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **0번이다.** 헤더의 재시도 횟수가 이미 한도라 곧바로 DLQ로 간다.

2. **그 소비자들은 구버전 라이브러리라 헤더를 확인하지 않았다.**

3. **추적 id·원 발행 시각까지 사라진다.** 재시도 횟수만 초기화한다.

4. **재처리 후 DLQ에 다시 들어온 메시지의 헤더와 소비자 처리 로그를 본다.** 처리 로그가 없고 재시도 횟수가 한도면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 헤더 그대로 재발행
① 문제 코드
```java
producer.send(target, msg.key(), msg.value(), msg.headers());
```
② 고친 코드
```java
Headers h = msg.headers().copy();
h.put("x-original-retry-count", h.get("x-retry-count"));
h.put("x-retry-count", "0");
producer.send(target, msg.key(), msg.value(), h);
report.count(target, "redriven");
```
무엇이 깨졌나: 재처리 도구와 소비자가 같은 헤더에 다른 의미(이력 vs 남은 기회)를 두었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 재시도 횟수 초기화 + 원값 보존 | 헤더 규약 | 작음 | 없음 | 기본 |
| 재처리 결과 건수 보고 | 집계 | 작음 | 없음 | 함께 적용 |

**결론**: 재처리는 재시도 기회를 새로 주고, 처리 결과 건수로 끝을 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
