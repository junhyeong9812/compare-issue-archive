# issue/cashrcpt/sigterm-inflight-duplicate-issue — 정답

태그: `deploy` · `idempotency`

## 정답
<!-- 질문 1:1 대응 -->

1. **ack 전에 프로세스가 죽어 메시지가 재전달됐다.**

2. **이중 발급이 났을 것이다.** 메시지 처리는 최소 한 번 전달이므로 멱등 키가 필수다.

3. **'이미 처리됨'으로 보고 원 결과를 조회해 저장하고 ack한다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```java
// 종료 훅 없음
```
② 고친 코드
```java
@PreDestroy void stop(){ consumer.pause(); inflight.awaitTermination(30, SECONDS); }
// DUPLICATE 응답 → lookup(requestKey) 결과 저장 후 ack
```
무엇이 깨졌나: 종료 시점 처리 중 작업을 고려하지 않았다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| graceful shutdown + 중복 응답 성공 처리 | 작음 | 없음 | 기본 |
| 배포를 야간에만 | 운영 제약 | 원인 잔존 | 선택하지 않음 |

**결론**: 메시지 소비자는 멱등하게 만들고 종료 시 처리 중 작업을 마친다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
