# issue/tokvault/replica-lag-token-not-found — 정답

태그: `consistency` · `replication`

## 정답
<!-- 질문 1:1 대응 -->

1. **몇 초 뒤엔 복제가 따라잡아 복제본에도 매핑이 있었다.**

2. **주 DB 부하가 늘어난다.** 없음일 때만 주 DB로 재조회하는 방식으로 부하를 줄였다.

3. **쓰기 직후 읽기인지, 읽기가 어디로 가는지를 본다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```java
return replica.findToken(t).orElseThrow(TokenNotFound::new);
```
② 고친 코드
```java
return replica.findToken(t).or(() -> primary.findToken(t)).orElseThrow(TokenNotFound::new);
```
무엇이 깨졌나: 복제 지연을 고려하지 않고 쓰기 직후 읽기를 복제본에 보냈다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 없음 시 주 DB 재조회 | 작음 | 주 DB 부하 소폭 | 기본 |
| 전부 주 DB | 작음 | 부하 | 선택하지 않음 |

**결론**: 쓰기 직후 읽기는 주 DB에서 확인할 수 있게 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
