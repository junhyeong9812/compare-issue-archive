# issue/notifyhub/http2-single-connection-stream-cap — 정답

태그: `performance` · `capacity`

## 정답
<!-- 질문 1:1 대응 -->

1. **거의 그대로다.** 병목은 스레드가 아니라 연결 하나의 스트림 상한이다.

2. **평소 초당 수백 건이라 상한에 닿지 않았다.** 전 회원 공지 같은 일괄 발송에서만 드러났다.

3. **안 된다.** 플랫폼은 발신자별 연결 수를 제한하고 과도하면 차단한다 — 권장 범위 안에서 늘린다.

## 문제 구조 (추상화 코드)

### 변형 A — 단일 HTTP/2 연결
① 문제 코드
```java
PushClient client = PushClient.builder().endpoint(ep).build();   // 연결 1개
```
② 고친 코드
```java
PushClient bulk = PushClient.builder().endpoint(ep).connections(8).build();
PushClient txn  = PushClient.builder().endpoint(ep).connections(2).build();  // 거래 알림 분리
```
무엇이 깨졌나: 처리량 상한을 계산하지 않고 클라이언트 기본값(연결 1개)을 썼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 연결 풀 확대 | 플랫폼 연결 허용 | 작음 | 과다 시 차단 | 기본 |
| 거래·대량 풀 분리 | 클라이언트 2개 | 작음 | 설정 복잡 | 함께 적용 |

**결론**: 대량 발송 처리량은 연결 수 × 스트림 상한 ÷ 응답 시간으로 미리 계산하고, 거래 알림과 풀을 나눈다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
