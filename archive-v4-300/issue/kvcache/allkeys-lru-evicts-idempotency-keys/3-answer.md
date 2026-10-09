# issue/kvcache/allkeys-lru-evicts-idempotency-keys — 정답

태그: `data-loss` · `config`

## 정답
<!-- 질문 1:1 대응 -->

1. **축출될 수 있다.** 마지막 접근이 오래된 순으로 지워지므로 지급 직후 다시 읽히지 않는 멱등 키가 먼저 사라진다.

2. **프로모션 캐시가 메모리를 채워 축출이 시작됐기 때문이다.** 평소에는 `maxmemory`에 닿지 않아 축출이 없었다.

3. **안전하지 않다.** TTL 있는 키 중 남은 TTL이 짧은 것부터 지운다 — 멱등 키도 대상이다.

4. **DB 유니크 제약이 정본이다.** 지급 테이블에 `(payout_id)` 유니크를 두고, 캐시는 빠른 경로로만 쓴다.

## 문제 구조 (추상화 코드)

### 변형 A — 정합성 키를 축출되는 캐시에 둠
① 문제 코드
```java
if (cache.setIfAbsent("payout:" + id, "1", Duration.ofHours(24))) {
    bank.transfer(req);        // 키가 축출되면 재시도에서 다시 지급
}
```
② 고친 코드
```java
try {
    payoutRepo.insert(id, req);  // UNIQUE(payout_id)
    bank.transfer(req);
} catch (DuplicateKeyException e) {
    return payoutRepo.find(id);  // 이미 처리됨
}
```
무엇이 깨졌나: 캐시를 저장소처럼 썼다 — 캐시는 언제든 키를 잃을 수 있다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| DB 유니크 제약 | 스키마 변경 | 작음 | DB 쓰기 지연 | 기본 |
| 멱등 전용 noeviction 인스턴스 | 인스턴스 추가 | 중간 | 메모리 가득 시 쓰기 오류 | 고빈도 경로 |
| maxmemory 증설 | 비용 | 중간 | 다음 프로모션에 재발 | 선택하지 않은 방법 |

**결론**: 멱등성은 DB 제약으로 보장하고 캐시는 보조로만 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
