# issue/outboxd/sequence-commit-order-skip — 정답

태그: `silent-failure` · `concurrency`

## 정답
<!-- 질문 1:1 대응 -->

1. **1001은 발행되지 않는다.** 다음 폴링은 id > 1002만 본다.

2. **트랜잭션이 짧아 커밋 순서가 대체로 id 순서와 같았다.** 외부 호출이 느려진 날 역전이 늘었다.

3. **확률은 줄지만 5초보다 긴 트랜잭션은 여전히 빠진다.** 상태 기반 조회가 근본 대처다.

4. **누락 이벤트의 id와 그 앞뒤 발행 id, 그리고 커밋 시각을 본다.** 누락 id가 이미 발행된 더 큰 id보다 늦게 커밋됐다면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 마지막 id 이후만 조회
① 문제 코드
```sql
SELECT * FROM outbox WHERE id > :last_id ORDER BY id LIMIT 500;
```
② 고친 코드
```sql
SELECT * FROM outbox WHERE published = false
 ORDER BY id LIMIT 500 FOR UPDATE SKIP LOCKED;
-- 발행 후 UPDATE outbox SET published = true WHERE id = ANY(:ids);
```
무엇이 깨졌나: id 단조 증가를 "커밋 순서"로 착각해 진행 위치로 썼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| published 상태 기반 | 인덱스(published=false) | 작음 | 갱신 부하 | 기본 |
| 지연 창 폴링 | 없음 | 작음 | 긴 트랜잭션 누락 | 선택하지 않음 |
| 로그 기반 CDC로 전환 | CDC 인프라 | 큼 | 운영 복잡 | 대규모 |

**결론**: 아웃박스는 행 상태로 미발행을 찾고, 하류는 이벤트 id로 멱등하게 받는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
