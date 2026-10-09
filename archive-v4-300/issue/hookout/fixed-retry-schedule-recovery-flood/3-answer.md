# issue/hookout/fixed-retry-schedule-recovery-flood — 정답

태그: `retry-storm` · `performance`

## 정답
<!-- 질문 1:1 대응 -->

1. **밀린 18만 건 대부분이다.** 재시도 시각이 같은 창에 모여 있다.

2. **초당 수천 건이 단일 출발지에서 왔다.** 가맹점 WAF가 우리 IP를 차단해 이후 정상 웹훅까지 막혔다.

3. **반열림에서 닫힘으로 바뀌는 순간 같은 폭주가 생길 수 있다.** 가맹점별 속도 제한이 함께 있어야 한다.

## 문제 구조 (추상화 코드)

### 변형 A — 고정 간격 재시도, 가맹점별 제한 없음
① 문제 코드
```java
retryAt = failedAt.plus(Duration.ofMinutes(10));
```
② 고친 코드
```java
long base = Math.min(3600, 30L << attempt);                       // 30s, 60s, ... 최대 1h
retryAt = failedAt.plusSeconds((long) (base * ThreadLocalRandom.current().nextDouble(0.5, 1.5)));
// 발송 시 merchantLimiter(merchantId).acquire()  — 가맹점별 초당 50건
```
무엇이 깨졌나: 실패 처리를 건 단위로만 설계했다 — 상대 단위의 부하를 보지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 가맹점별 속도 제한 | 가맹점 한도 설정 | 중간 | 밀림 해소 지연 | 기본 |
| 백오프 + 지터 | 없음 | 작음 | 첫 재시도는 여전히 몰림 | 함께 적용 |
| 회로 차단 | 상태 저장 | 중간 | 복구 감지 지연 | 함께 적용 |

**결론**: 재시도는 상대 단위로 속도를 제한하고, 시각은 흩는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
