# issue/notifyhub/ios-sandbox-endpoint-prod-build — 정답

태그: `environment-drift` · `deploy`

## 정답
<!-- 질문 1:1 대응 -->

1. **전부 `400 BadDeviceToken`이다.** Android는 다른 설정 키를 써서 정상이었다.

2. **토큰이 이미 삭제됐기 때문이다.** 앱을 다시 켜 토큰을 재등록하기 전까지 받을 수 없었다 — 복구에 2주가 걸렸다.

3. **개별 건은 맞다.** 다만 짧은 시간에 비율이 평소(0.1%)보다 크게 오르면 설정 문제일 가능성이 높으므로 삭제를 멈춘다.

## 문제 구조 (추상화 코드)

### 변형 A — 환경 의존 값에 코드 기본값
① 문제 코드
```java
String endpoint = cfg.get("ios.endpoint", "https://api.sandbox.push.apple.com");
```
② 고친 코드
```java
String endpoint = cfg.require("ios.endpoint");                    // 없으면 기동 실패
if (profile.isProd() && endpoint.contains("sandbox")) throw new IllegalStateException(endpoint);
// 삭제 회로: 10분 창에서 무효 비율 > 2% 이면 삭제 중지 + 경보
```
무엇이 깨졌나: 운영에서 틀리면 안 되는 값에 개발용 기본값을 두었고, 실패 응답에 대한 파괴적 후처리에 상한이 없었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 필수 키 + 기동 검사 | 설정 스키마 | 작음 | 없음 | 기본 |
| 삭제 정지 회로 | 비율 집계 | 작음 | 실제 대량 무효 시 지연 | 함께 적용 |

**결론**: 환경 의존 값은 기본값 없이 필수로 받고, 파괴적 후처리에는 비율 상한을 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
