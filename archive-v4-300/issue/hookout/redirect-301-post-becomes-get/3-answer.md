# issue/hookout/redirect-301-post-becomes-get — 정답

태그: `silent-failure` · `http-semantics`

## 정답
<!-- 질문 1:1 대응 -->

1. **본문 없는 GET이다.** 결제 정보는 전달되지 않았다.

2. **리다이렉트 뒤 최종 응답 코드만 기록했다.** GET에 대한 200을 웹훅 수신 확인으로 오인했다.

3. **메서드와 본문이 유지돼 정상 전달됐을 것이다.** 다만 서명 검증 대상 URL이 달라질 수 있어 리다이렉트 추적 자체를 끄는 쪽을 택했다.

4. **발송 로그의 리다이렉트 이력(첫 응답 코드)을 본다.** 첫 응답이 3xx면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 리다이렉트 자동 추적
① 문제 코드
```java
HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
```
② 고친 코드
```java
HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build();
// 3xx → FAILED(REDIRECT) + Location 기록, 가맹점에 URL 갱신 요청
```
무엇이 깨졌나: "서버가 받았다"를 최종 응답 코드로 판정했는데, 그 응답은 다른 요청에 대한 것이었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 리다이렉트 미추적 + 실패 기록 | 가맹점 URL 갱신 협조 | 작음 | 갱신 전까지 실패 | 기본 |
| 307/308만 추적 | 클라이언트 설정 | 작음 | 서명 URL 불일치 | 선택하지 않음 |

**결론**: 웹훅은 리다이렉트를 따라가지 않고, 3xx는 가맹점 설정 문제로 알린다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
