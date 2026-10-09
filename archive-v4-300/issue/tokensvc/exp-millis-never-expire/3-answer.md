# issue/tokensvc/exp-millis-never-expire — 정답

태그: `security` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **사실상 만료되지 않는다.** 초로 읽으면 수만 년 뒤다.

2. **파트너 SDK 4.0이 `Date.now()`(밀리초)를 그대로 썼다.**

3. **이미 만료된 1970년 시각으로 읽혀 모든 토큰이 거부된다.** 그 경우는 곧바로 장애로 보인다.

4. **정상 토큰이 통과하는지만 시험했다.** 만료 토큰 시험은 직접 만든 토큰으로 했다.

5. **`exp ≤ now + 15분`, `exp − iat ≤ 15분` 검사.** 위반 시 거부하고 파트너별 경보. 파트너에 SDK 수정을 요청했다.

## 문제 구조 (추상화 코드)

### 변형 A — 미래인지만 확인
① 문제 코드
```java
if (claims.exp() < Instant.now().getEpochSecond()) throw expired();
```
② 고친 코드
```java
long now = Instant.now().getEpochSecond();
if (claims.exp() < now) throw expired();
if (claims.exp() > now + 900 || claims.exp() - claims.iat() > 900)
    throw invalid("exp out of range");
```
무엇이 깨졌나: 외부가 만든 값의 단위를 믿고 한쪽 경계만 검사했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| exp 상한·수명 검사 | 코드 수정 | 작음 | 시계 차이 | 기본 |
| 파트너 SDK 수정 요청 | 파트너 협조 | 중간 | 지연 | 함께 |
| 음성 시험(만료·과대 exp) | 시험 케이스 | 작음 | 없음 | 재발 방지 |

**결론**: 시간 값은 양쪽 경계를 검사한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
