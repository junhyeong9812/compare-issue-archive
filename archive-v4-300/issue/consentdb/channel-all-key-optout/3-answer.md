# issue/consentdb/channel-all-key-optout — 정답

태그: `data-semantics` · `privacy`

## 정답
<!-- 질문 1:1 대응 -->

1. **발송된다.** ALL이 Y라서 SMS 철회가 무시된다.

2. **가입 화면 철회는 ALL을 N으로 바꾸기 때문이다.**

3. **같은 문제가 있었다.** 다만 푸시는 OS 알림 설정으로 끈 고객이 많아 민원이 적었다.

4. **발송 서비스 로그에는 'ALL=Y' 판정만 남아 정상으로 보였다.** 9월 민원 11건 뒤에 조사해 철회 후 발송 약 2만 건을 확인했다.

5. **consentdb `can-send(customer, purpose, channel)` 판정 API.** 발송 서비스는 키를 직접 읽지 않는다. 채널이 추가돼도 판정 API만 고친다.

## 문제 구조 (추상화 코드)

### 변형 A — 발송 쪽이 키를 직접 해석
① 문제 코드
```java
if (consent.get(cid, "mkt.ALL") == Y) sms.send(cid, ad);
```
② 고친 코드
```java
if (consent.canSend(cid, Purpose.PROMO, Channel.SMS)) sms.send(cid, ad);
// canSend = ALL==Y && SMS!=N  (consentdb 안에서만 정의)
```
무엇이 깨졌나: 동의 모델이 채널별로 세분됐는데, 판정 규칙이 소비자 쪽에 흩어져 있어 함께 바뀌지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 판정 API 일원화 | API 신설 | 중간 | API 장애 시 발송 중단 | 기본 |
| 발송 쪽 판정 수정 | 코드 수정 | 작음 | 다른 소비자 누락 | 임시 |
| 철회 후 발송 일일 점검 | 로그 조인 | 작음 | 사후 | 안전망 |

**결론**: 동의 판정은 한 곳에서 하고, 장애 시 발송을 멈추는 쪽으로 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
