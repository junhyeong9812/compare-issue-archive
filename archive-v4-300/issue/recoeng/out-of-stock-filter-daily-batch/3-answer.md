# issue/recoeng/out-of-stock-filter-daily-batch — 정답

태그: `data-freshness` · `ux` · `conversion`

## 정답
<!-- 질문 1:1 대응 -->

1. **다음 날 04:00 배치 때 반영됐다.**

2. **추천은 재고 서비스를 조회하지 않고 새벽 스냅샷 목록을 썼다.**

3. **상세·검색은 요청마다 재고 서비스를 조회했다.**

4. **품절 이벤트 구독으로 제외 목록 즉시 갱신, 응답 직전 재고 일괄 확인(상위 20개).**

## 문제 구조 (추상화 코드)

### 변형 A — 일 배치 스냅샷으로 실시간 속성 필터
① 문제 코드
```python
exclude = load_snapshot("oos", day=today())
return [i for i in ranked if i not in exclude]
```
② 고친 코드
```python
exclude = oos_set_live()                 # 품절 이벤트로 갱신되는 집합
items = [i for i in ranked if i not in exclude][:40]
return [i for i in items if stock.in_stock_bulk(items)[i]][:20]
```
무엇이 깨졌나: 자주 바뀌는 속성을 하루 단위 스냅샷으로 걸러 신선도가 업무 요구에 못 미쳤다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 품절 이벤트 구독 | 이벤트 발행 | 중간 | 이벤트 유실 | 기본 |
| 응답 직전 일괄 확인 | 재고 서비스 용량 | 작음 | 지연 증가 | 기본 |
| 배치 주기 1시간 | — | 작음 | 타임세일엔 여전히 늦음 | 임시 |

**결론**: 필터의 신선도는 걸러야 할 속성이 바뀌는 속도에 맞춘다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
