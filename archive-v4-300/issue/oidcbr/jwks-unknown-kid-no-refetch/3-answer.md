# issue/oidcbr/jwks-unknown-kid-no-refetch — 정답

태그: `caching` · `availability`

## 정답
<!-- 질문 1:1 대응 -->

1. **실패한다.** 모르는 kid라 키를 찾지 못한다.

2. **매일 14:00 정기 갱신에서 새 키를 받았다.** 장애 약 9시간, 로그인 실패 약 21만 건.

3. **다른 IdP는 회전 전 새 키를 미리 JWKS에 올려 며칠 겹치게 했다.**

4. **공격자가 임의 kid를 보내 IdP로의 요청을 폭주시킬 수 있다.** 그래서 재조회 최소 간격이 필요하다.

5. **모르는 kid → 재조회(최소 5분 간격) → 그래도 없으면 실패. 캐시 수명은 Cache-Control(최대 24시간).** 검증 실패율 경보를 IdP별로 둔다.

## 문제 구조 (추상화 코드)

### 변형 A — 모르는 kid 즉시 실패
① 문제 코드
```java
Key k = cache.get(kid);
if (k == null) throw new InvalidToken("unknown kid");
```
② 고친 코드
```java
Key k = cache.get(kid);
if (k == null && refetchGate.tryAcquire(Duration.ofMinutes(5))) {
    cache.replace(jwks.fetch());
    k = cache.get(kid);
}
if (k == null) throw new InvalidToken("unknown kid");
```
무엇이 깨졌나: 외부 키 회전을 캐시 주기에 맞춰 일어날 것으로 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 모르는 kid 재조회 + 최소 간격 | 코드 수정 | 작음 | 간격 내 실패 | 기본 |
| Cache-Control 준수 | 코드 수정 | 작음 | IdP 헤더 부정확 | 함께 |
| IdP별 실패율 경보 | 지표 | 작음 | 없음 | 함께 |

**결론**: 검증 키 캐시는 '모르는 키'를 갱신 신호로 쓴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
