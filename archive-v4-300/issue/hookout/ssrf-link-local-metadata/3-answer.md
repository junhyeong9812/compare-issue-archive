# issue/hookout/ssrf-link-local-metadata — 정답

태그: `security` · `ssrf`

## 정답
<!-- 질문 1:1 대응 -->

1. **아니다.** 그 도메인이 내부 IP로 해석되도록 바뀔 수 있다 — 발송 시점의 실제 IP로 검사해야 한다.

2. **요청만 가는 SSRF를 "읽을 수 있는" SSRF로 만들었다.**

3. **리다이렉트를 따라가지 않거나, 각 단계마다 IP를 검사해야 한다.**

## 문제 구조 (추상화 코드)

### 변형 A — 등록 시 형식만 검사
① 문제 코드
```java
if (!url.startsWith("http")) throw new InvalidUrl();
```
② 고친 코드
```java
InetAddress ip = resolver.resolveOnce(uri.getHost());          // 발송 직전 해석
if (ip.isSiteLocalAddress() || ip.isLinkLocalAddress() || ip.isLoopbackAddress() || ip.isAnyLocalAddress())
    throw new ForbiddenDestination(ip);
http.connect(ip, uri);                                          // 해석한 IP 로 고정 연결
```
무엇이 깨졌나: 신뢰할 수 없는 입력(가맹점 URL)이 내부 네트워크 요청의 목적지가 되는데 목적지 검사가 없었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 발송 시점 IP 검사 + 고정 연결 | 리졸버 제어 | 중간 | IPv6 표기 누락 | 기본 |
| 송신 전용 프록시(외부만 허용) | 프록시 운영 | 중간 | 프록시 장애 | 함께 적용 |

**결론**: 가맹점이 정하는 목적지는 발송 직전에 실제 IP로 검사하고, 응답 내용은 돌려주지 않는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
