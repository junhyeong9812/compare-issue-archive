# issue/auditsink/shutdown-buffer-no-flush — 정답

태그: `silent-failure` · `lifecycle`

## 정답
<!-- 질문 1:1 대응 -->

1. **사라진다.** 마지막 flush 이후 쌓인 0~5초치 이벤트가 전송되지 않는다.

2. **앱이 SIGTERM을 받자마자 종료하도록 돼 있었다.** 유예 시간은 '최대'일 뿐 앱이 정리 작업을 하지 않으면 바로 끝난다.

3. **SIGKILL이라 hook도 못 돈다.** 이 경우까지 막으려면 이벤트를 로컬 디스크 스풀에 먼저 쓰고 재기동 시 보내야 한다.

4. **배포마다 파드 12개가 차례로 종료되며 각각 몇 초치를 잃었기 때문이다.** 1월 배포 23회, 차이 약 1,900건이었다.

5. **종료 hook에서 flush + 최대 10초 대기, 감사 이벤트는 디스크 스풀 후 전송.** 클라이언트 3.0이 `spool.dir`를 지원해 올렸다.

## 문제 구조 (추상화 코드)

### 변형 A — 종료 hook 없음
① 문제 코드
```java
AuditClient client = AuditClient.builder().flushEvery(5, SECONDS).build();
// main 종료 시 버퍼 방치
```
② 고친 코드
```java
AuditClient client = AuditClient.builder()
    .flushEvery(5, SECONDS).spoolDir("/var/spool/audit").build();
Runtime.getRuntime().addShutdownHook(new Thread(() -> client.flush(Duration.ofSeconds(10))));
```
무엇이 깨졌나: 비동기 버퍼의 수명이 프로세스 수명보다 짧게 끝날 수 있다는 경로를 처리하지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| shutdown hook flush | 코드 수정 | 작음 | SIGKILL | 기본 |
| 디스크 스풀 | 볼륨 | 중간 | 디스크 가득 | 감사 이벤트 |
| 배포일 대사 경보 | 대사 배치 | 작음 | 사후 | 안전망 |

**결론**: 유실이 안 되는 이벤트는 종료 경로와 강제 종료 경로를 모두 설계한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
