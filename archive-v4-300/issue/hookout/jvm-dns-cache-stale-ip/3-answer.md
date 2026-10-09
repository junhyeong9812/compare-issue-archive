# issue/hookout/jvm-dns-cache-stale-ip — 정답

태그: `infra` · `caching`

## 정답
<!-- 질문 1:1 대응 -->

1. **옛 IP다.** 기동 이후 캐시를 갱신하지 않는다.

2. **다른 서비스는 기본 이미지가 달라 TTL 30초 정책이었다.**

3. **모든 요청이 DNS를 조회해 지연과 DNS 부하가 늘어난다.** 60초 정도가 균형점이었다.

## 문제 구조 (추상화 코드)

### 변형 A — 무기한 DNS 캐시
① 문제 코드
```properties
# java.security
networkaddress.cache.ttl=-1
```
② 고친 코드
```properties
networkaddress.cache.ttl=60
networkaddress.cache.negative.ttl=5
```
무엇이 깨졌나: 런타임의 숨은 기본 설정이 상대 시스템의 변경을 따라가지 못하게 했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| TTL 60s | 없음 | 작음 | 없음 | 기본 |
| 실패 시 재조회 래퍼 | 클라이언트 수정 | 중간 | 구현 복잡 | 추가 안전망 |

**결론**: 외부로 나가는 서비스는 JVM DNS 캐시 설정을 이미지 점검 항목에 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
