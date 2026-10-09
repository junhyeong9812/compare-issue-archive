# issue/tplsvc/cache-evict-competing-consumer — 정답

태그: `caching` · `consistency`

## 정답
<!-- 질문 1:1 대응 -->

1. **약 83건이다.** 6개 파드 중 1개만 캐시를 비웠다.

2. **로컬 캐시 TTL 24시간이 지나 다시 읽었다.**

3. **TTL 5분이 안전망이다.** 그리고 발송 기록의 템플릿 버전으로 놓친 파드를 찾을 수 있다.

4. **옛 문구로 나간 발송의 처리 파드를 본다.** 특정 파드들에만 몰리면 파드별 캐시 문제다.

## 문제 구조 (추상화 코드)

### 변형 A — 무효화를 경쟁 소비 큐로 전달
① 문제 코드
```java
@Listener(queue = "tpl.evict", group = "tplsvc")       // 그룹 공유 → 1개 파드만
void onEvict(String id) { cache.invalidate(id); }
```
② 고친 코드
```java
@Listener(topic = "tpl.evict", group = "tplsvc-${POD_NAME}")   // 파드별 구독
void onEvict(String id) { cache.invalidate(id); }
// cache TTL 5m, 발송 기록에 template_version 저장
```
무엇이 깨졌나: "모두에게 알림"이 필요한 메시지를 "하나가 처리" 의미의 채널로 보냈다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 파드별 구독 | 브로커 지원 | 작음 | 구독 정리 | 기본 |
| 짧은 TTL만 | 없음 | DB 조회 증가 | 최대 TTL 지연 | 보조 |

**결론**: 캐시 무효화는 브로드캐스트로 보내고, 발송 기록에 템플릿 버전을 남긴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
