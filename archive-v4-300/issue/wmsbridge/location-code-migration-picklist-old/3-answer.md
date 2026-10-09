# issue/wmsbridge/location-code-migration-picklist-old — 정답

태그: `migration` · `operations` · `latency`

## 정답
<!-- 질문 1:1 대응 -->

1. **개편 전 옛 로케이션 코드가 찍혔다.**

2. **리스트 생성이 전날 저녁 적재한 로케이션 캐시를 썼다.**

3. **1센터는 로케이션 개편이 없었다.**

4. **마스터 변경 이벤트로 캐시 무효화, 개편 전날 리스트 리허설, 현장 대조표.**

## 문제 구조 (추상화 코드)

### 변형 A — 마스터 변경과 무관한 주기 캐시
① 문제 코드
```java
@Scheduled(cron = "0 0 7 * * *") void loadLocations() { cache.load(); }
```
② 고친 코드
```java
@Scheduled(cron = "0 0 7 * * *") void loadLocations() { cache.load(); }
@EventListener void onMasterChanged(LocationMasterChanged e) { cache.load(); }
```
무엇이 깨졌나: 운영 마스터가 바뀌는 사건이 캐시 갱신과 연결돼 있지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 변경 이벤트로 무효화 | 이벤트 발행 | 작음 | 이벤트 유실 | 기본 |
| 개편 전날 리허설 | 절차 | 작음 | 생략 | 기본 |
| 캐시 제거 | WMS 조회 용량 | 중간 | 피크 부하 | 선택하지 않은 방법 |

**결론**: 마스터 변경은 캐시까지 끝나야 끝난 것이다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
