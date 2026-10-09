# issue/hookin/arrival-order-status-overwrite — 정답

태그: `ordering` · `data-semantics`

## 정답
<!-- 질문 1:1 대응 -->

1. **APPROVED다.** 도착한 통지를 그대로 반영했다.

2. **재전송이 생겨야 순서가 뒤바뀐다.** 그날 5분 장애 동안 재전송된 승인 통지 중 그 사이 취소된 87건이 되돌려졌다.

3. **`seq`가 같으므로 두 번째는 무시된다(멱등).**

4. **차액 건의 통지 수신 이력을 시간순으로 본다.** 취소 뒤 승인이 다시 수신됐다면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 도착 순서 덮어쓰기
① 문제 코드
```java
payment.setStatus(notice.status());
repo.save(payment);
```
② 고친 코드
```java
if (notice.seq() <= payment.lastSeq()) { audit.stale(notice); return; }
if (!payment.status().canTransitTo(notice.status())) { audit.illegal(notice); return; }
payment.apply(notice.status(), notice.seq());
repo.save(payment);                                       // 낙관적 잠금 버전 포함
```
무엇이 깨졌나: 외부 통지의 도착 순서를 발생 순서로 착각했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| seq 비교 + 상태 전이 규칙 | PG가 seq 제공 | 작음 | 없음 | 기본 |
| 통지 받으면 PG 조회 API로 현재 상태 확인 | 조회 API | 호출 증가 | 조회 지연 | 고액 거래 |

**결론**: 외부 통지는 순번과 상태 전이 규칙으로 걸러 반영하고, 애매하면 PG 조회로 확정한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
