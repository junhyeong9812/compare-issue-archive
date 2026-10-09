# issue/settlebat/container-timezone-utc-date-shift — 정답

태그: `deploy` · `timezone`

## 정답
<!-- 질문 1:1 대응 -->

1. **JVM 기본 시간대가 이미지 설정을 따랐다.**

2. **당장은 되지만 다음 이미지에서 또 빠질 수 있다.** 코드에서 시간대를 명시했다.

3. **전송 전 검증(파일 날짜 = 기대 정산일)에서 걸러 재생성했다.** 외부 전송은 없었다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
LocalDate settleDate = LocalDate.now().minusDays(1);
```
② 고친 코드
```java
static final ZoneId KST = ZoneId.of("Asia/Seoul");
LocalDate settleDate = LocalDate.now(clock.withZone(KST)).minusDays(1);
```
무엇이 깨졌나: 업무 날짜가 실행 환경의 암묵 설정에 의존했다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 코드에서 시간대 명시 | 작음 | 없음 | 기본 |
| 이미지 TZ 설정 | 작음 | 다음 이미지 누락 | 보조 |

**결론**: 업무 날짜는 명시한 시간대로 계산한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
