# issue/mailrelay/mailbox-full-hard-bounce-suppress — 정답

태그: `silent-failure` · `classification`

## 정답
<!-- 질문 1:1 대응 -->

1. **API는 `202`, 실제 전송은 없다.** 주소가 suppression 목록에 영구로 남아 있어 릴레이가 버린다.

2. **API 응답과 서비스 로그에는 실패가 없었다.** suppressed는 INFO 로그였고 지표에도 없었다. 목록에 쌓인 주소는 약 9,400개였고 그중 6,100개가 552 계열이었다.

3. **영구 반송이 맞다.** 존재하지 않는 주소에 계속 보내면 발신 평판이 떨어진다. 문제는 5xx 전체를 같은 부류로 본 것이다.

4. **해당 주소의 suppression 등록 여부와 등록 사유 코드를 본다.** `mailrelay suppress show <주소>`에 552가 보이면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 첫 자리로만 반송 분류
① 문제 코드
```java
boolean hard = smtpCode.startsWith("5");
if (hard) suppression.addPermanent(addr);
```
② 고친 코드
```java
BounceKind k = BounceKind.fromEnhanced(enhancedCode);    // 5.1.1 → HARD, 5.2.2 → SOFT
if (k == HARD) suppression.addPermanent(addr, enhancedCode);
else softCounter.hit(addr);                              // 7일 내 3회 → 30일 일시 차단
```
무엇이 깨졌나: 반송 분류를 기본 응답 코드 첫 자리로만 했다 — 확장 코드가 말하는 "일시적"을 무시했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 확장 코드 기준 분류 | 수신 서버가 확장 코드를 줌 | 분류표 관리 | 확장 코드 없는 서버 | 기본 |
| suppressed 를 서비스에 통지 | 콜백 채널 | 연동 작업 | 없음 | 함께 적용 |
| 기존 552 등록분 일괄 해제 | 등록 사유 보존 | 1회 작업 | 실제 폐기 주소 섞임 | 사후 정리 |

**결론**: 반송은 확장 코드로 나누고, 차단 결정은 서비스가 볼 수 있게 통지한다. 기존 552 등록분은 해제했다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
