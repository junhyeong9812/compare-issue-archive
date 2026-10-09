# issue/srchidx/bulk-200-partial-errors-ignored — 정답

태그: `silent-failure` · `partial-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **HTTP 200, 본문 `errors: true`**이고 7개 item이 `status: 400`·`mapper_parsing_exception`이다.

2. **해외 결제 어댑터에서 온 거래다.** 이 어댑터만 금액을 서식 문자열로 넘겼다 — 하루 평균 120건이 빠졌다.

3. **같이 거부된다.** 나머지 993건은 덮어쓰기라 무해하다.

4. **원장 건수와 인덱스 건수를 시간 구간별로 비교한다.** 한 시간이 지나도 차이가 줄지 않으면 거부다.

5. **item별 status를 보고 실패 문서를 DLQ에 보낸다.** 실패가 1건이라도 있으면 배치 지표에 올린다.

## 문제 구조 (추상화 코드)

### 변형 A — bulk 응답의 HTTP 상태만 확인
① 문제 코드
```python
r = es.bulk(body=actions)
if r.status_code == 200:
    batch.ok()
```
② 고친 코드
```python
r = es.bulk(body=actions).json()
failed = [i for i in r["items"] if i["index"]["status"] >= 300]
for f in failed:
    dlq.put(f)                      # 원문과 오류 사유 보존
batch.done(ok=len(actions) - len(failed), failed=len(failed))
```
무엇이 깨졌나: 부분 실패가 가능한 API의 응답을 전부 성공/전부 실패로 해석했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| item별 상태 확인 + DLQ | DLQ 저장소 | 작음 | DLQ 방치 | 기본 |
| 원장·인덱스 건수 대사 | 집계 쿼리 | 작음 | 발견 지연(1시간) | 안전망 |
| 어댑터에서 금액 정규화 | 어댑터 수정 | 작음 | 다른 어댑터 동일 실수 | 원인 제거 |

**결론**: 부분 실패를 문서 단위로 처리하고, 건수 대사를 안전망으로 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
