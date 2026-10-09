# issue/ordhub/callback-idempotency-ttl-double-order — 정답

태그: `idempotency` · `retry` · `data-integrity`

## 정답
<!-- 질문 1:1 대응 -->

1. **11 ~ 18분.**\
결제 대행 쪽 콜백 큐가 밀리며 재시도가 멱등 키 보관 시간(10분)을 넘겨 도착했다.

2. **멱등 키가 10분짜리 캐시에만 있었다.**\
만료 뒤 도착한 재시도를 새 결제로 봤다.

3. **평시 재시도는 1분 이내라 키가 살아 있었다.**\
결제 대행 적체(할로윈 이벤트)가 처음으로 간격을 벌렸다.

4. **주문 테이블에 결제 ID 유일 제약을 걸고 위반은 "기존 주문 반환"으로 처리.**\
이미 나간 63건은 택배 회수와 고객 안내로 정리했다.

## 문제 구조 (추상화 코드)

### 변형 A — 캐시 TTL에만 기댄 멱등성
① 문제 코드
```java
if (cache.putIfAbsent("pay:" + payId, "1", Duration.ofMinutes(10))) {
    orderRepo.create(order);
}
```
② 고친 코드
```java
try {
    orderRepo.create(order);            // payment_id UNIQUE
} catch (DuplicateKeyException e) {
    return orderRepo.findByPaymentId(payId);
}
```
무엇이 깨졌나: 멱등성을 기한 있는 캐시에 맡겨 상대의 지연이 기한을 넘는 순간 깨졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 결제 ID 유일 제약 | 스키마 변경 | 작음 | 기존 중복 데이터 정리 필요 | 기본 |
| 캐시 보관 24시간 | 메모리 | 작음 | 캐시 장애 시 무방비 | 보조 |
| 콜백 응답 빠르게(비동기 처리) | 큐 도입 | 중간 | 처리 실패 추적 | 병행 |

**결론**: 멱등성은 영구 저장소의 제약으로 지킨다 — 캐시는 보조다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
