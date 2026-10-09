# issue/etaxbr/sync-issue-timeout-thread-block — 정답

태그: `performance` · `coupling`

## 정답
<!-- 질문 1:1 대응 -->

1. **같은 스레드 풀을 써서다.** 가입 요청이 스레드를 30초씩 잡아 다른 API가 스레드를 못 얻었다.

2. **완화는 되지만 실패한 가입이 남는다.** 중계사 장애가 가입 실패가 된다. 분리가 근본이다.

3. **가입 트랜잭션에서 아웃박스 행을 같이 커밋한다.** 워커가 처리 후 상태를 바꾸고, 하루 넘은 미처리 행은 경보한다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
@Transactional public void join(Req r){ save(r); payFee(r); etax.issue(r.toInvoice()); }
```
② 고친 코드
```java
@Transactional public void join(Req r){ save(r); payFee(r); outbox.add(InvoiceEvent.of(r)); }
// InvoiceWorker: outbox 폴링 → etax.issue, 실패 시 백오프
```
무엇이 깨졌나: 부가 작업을 핵심 요청 경로에 동기로 묶었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 아웃박스 + 비동기 워커 | 중간 | 워커 지연 | 기본 |
| 타임아웃 단축만 | 작음 | 가입 실패 | 임시 |

**결론**: 외부 부가 작업은 요청 경로에서 분리한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
