# issue/connpx/txn-pool-session-timezone-leak — 정답

태그: `contract-drift` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **전날 날짜가 저장될 수 있다.** 백엔드 커넥션 #7의 세션 시간대가 UTC로 남아 있어 `now()::date`가 UTC 날짜를 낸다.\
KST 00:00~08:59 거래는 UTC로 아직 전날이다.

2. **2024-03-04에 리포트 서비스가 `connpx` 경유로 바뀌었기 때문이다.** 그 전엔 직접 접속이라 세션 설정이 자기 커넥션에만 머물렀다.

3. **세션 풀링이면 생기지 않는다** — 클라이언트가 끊길 때 `server_reset_query`(`DISCARD ALL`)가 돈다.\
`SET LOCAL`은 트랜잭션이 끝나면 사라지므로 트랜잭션 풀링에서도 안전하다.

4. **거래일 귀속 문제다.** 이틀을 합치면 맞고 하루씩은 어긋나는 대사는 누락이 아니라 날짜 경계 이동이다.\
차액 건은 전부 KST 자정~09시였다.

5. **프록시**: 세션 상태를 바꾸는 서비스는 세션 모드 풀로 분리한다. **앱**: 거래일을 KST로 계산해 넣는다.

## 문제 구조 (추상화 코드)

### 변형 A — 세션 SET이 공유 커넥션에 남음
① 문제 코드
```java
jdbc.execute("SET TIME ZONE 'UTC'");            // 트랜잭션 풀링: 다음 클라이언트에 남는다
report = jdbc.query(DAILY_SQL);
// 승인 서버
jdbc.update("INSERT INTO txn(day, ...) VALUES (now()::date, ...)");
```
② 고친 코드
```java
jdbc.execute("SET LOCAL TIME ZONE 'UTC'");      // 트랜잭션 안에서만 유효
report = jdbc.query(DAILY_SQL);
// 승인 서버
LocalDate day = LocalDate.now(ZoneId.of("Asia/Seoul"));
jdbc.update("INSERT INTO txn(day, ...) VALUES (?, ...)", day);
```
무엇이 깨졌나: 세션 상태가 커넥션 단위라는 전제가 트랜잭션 풀링에서는 성립하지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| `SET LOCAL`로 교체 | 트랜잭션 안에서 실행 | 작음 | 트랜잭션 밖 SET이 다시 들어옴 | 기본 |
| 거래일을 앱에서 계산 | 코드 수정 | 작음 | 다른 서비스가 now() 사용 | 모든 날짜 귀속 경로 |

**결론**: 세션 SET을 트랜잭션 범위로 줄이고, 거래일은 앱이 명시적 시간대로 넣는다. 둘 중 하나만 하면 다른 서비스가 다시 밟는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
