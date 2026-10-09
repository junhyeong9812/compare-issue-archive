# issue/pgroute/hikari-pool-exhaust-pg-latency — 정답

태그: `performance` · `resource-exhaustion`

## 정답
<!-- 질문 1:1 대응 -->

1. **모든 요청이 커넥션을 쥔 채 PG를 기다렸기 때문이다.** 동시 필요 커넥션이 풀 크기의 10배 가까이 됐다.

2. **아니다.** DB 서버 max_connections가 300이고 다른 서비스와 나눠 쓴다. 풀을 늘리면 DB가 먼저 거부한다.

3. **PG 호출 결과를 모르는 '요청됨' 상태다.** 서버가 그 사이 죽으면 결과 조회 배치가 PG에 물어 정리한다.

4. **부하 시험의 PG 모의 서버가 고정 200ms로 응답했다.** 지연 주입 시나리오(1s·5s)를 추가했다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
@Transactional
public Result pay(Req r) { repo.insert(r); var res = pg.approve(r); repo.update(res); return res; }
```
② 고친 코드
```java
public Result pay(Req r) {
  long id = tx.execute(s -> repo.insertRequested(r));   // 커밋
  var res = pg.approve(r);                              // 트랜잭션 밖
  tx.execute(s -> repo.complete(id, res));
  return res;
}
```
무엇이 깨졌나: 외부 지연을 DB 자원 점유로 전파하는 구조였다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 트랜잭션 분리 + 결과 조회 배치 | 중간 | 요청됨 상태 처리 필요 | 기본 |
| 풀 크기 확대 | 작음 | DB 연결 한도 | 선택하지 않음 |

**결론**: 외부 호출은 트랜잭션 밖에서 하고, 중간 상태를 정리하는 경로를 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
