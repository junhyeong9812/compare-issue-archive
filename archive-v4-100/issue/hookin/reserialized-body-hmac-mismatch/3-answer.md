# issue/hookin/reserialized-body-hmac-mismatch — 정답

태그: `auth` · `encoding`

## 정답
<!-- 질문 1:1 대응 -->

1. **실패한다.** 재직렬화가 `/`로 바꿔 바이트가 달라진다.

2. **테스트 통지는 우리가 같은 직렬화기로 만들었다.** 원문과 재직렬화가 항상 같았다.

3. **실패한다.** 순서·공백·숫자 표기(1.0 vs 1) 모두 바이트를 바꾼다.

## 문제 구조 (추상화 코드)

### 변형 A — 재직렬화 본문으로 서명 검증
① 문제 코드
```java
String payload = objectMapper.writeValueAsString(notification);
verify(hmac(secret, payload), header.signature());
```
② 고친 코드
```java
byte[] raw = cachedBody.raw();                                 // 수신 원문 그대로
if (!MessageDigest.isEqual(hmac(secret, raw), decode(header.signature())))
    throw new InvalidSignature();
```
무엇이 깨졌나: 서명 대상(바이트)과 검증 대상(의미)을 혼동했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 원문 바이트 캐시 | 필터 순서 제어 | 작음 | 없음 | 기본 |
| PG에 정규화 규격 요청 | PG 협조 | 큼 | 불가 | 선택하지 않음 |

**결론**: 서명 검증은 수신 원문 바이트로, 비교는 상수 시간으로 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
