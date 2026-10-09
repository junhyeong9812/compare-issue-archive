# issue/consentdb/export-delta-misses-withdraw — 정답

태그: `sync` · `privacy`

## 정답
<!-- 질문 1:1 대응 -->

1. **포함되지 않는다.** `agreed_at`이 바뀌지 않았다.

2. **빠지지 않는다.** 증분은 추가만 하고 월 전체 추출도 병합이라, 4월 대사(4월 22일) 전까지 남아 있었다.

3. **재동의는 `agreed_at`이 갱신돼 증분에 잡힌다.** 철회만 빠지는 비대칭이었다.

4. **제휴사 적재기가 전체 추출을 덮어쓰지 않고 병합했다.** 계약서에는 '전체 교체'로 적혀 있었지만 구현이 달랐다. 철회자 약 6,400명이 명단에 남았다.

5. **`updated_at`(행 변경 시각) + 행 상태(동의/철회).** 철회는 `op=D` 레코드로 보내고, 월 1회 차집합 대사를 제휴사와 합의했다.

## 문제 구조 (추상화 코드)

### 변형 A — 동의 시각 기준 증분
① 문제 코드
```sql
SELECT customer_id FROM consent
WHERE purpose = 'PARTNER_MKT' AND agreed_at > :last AND withdrawn_at IS NULL;
```
② 고친 코드
```sql
SELECT customer_id,
       CASE WHEN withdrawn_at IS NULL THEN 'U' ELSE 'D' END AS op
FROM consent
WHERE purpose = 'PARTNER_MKT' AND updated_at > :last;
```
무엇이 깨졌나: 증분의 기준을 '생성'에 두어 상태 변경(철회)이 전달 경로에서 사라졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| updated_at 증분 + 삭제 레코드 | 컬럼·트리거 추가 | 중간 | 트리거 누락 | 기본 |
| 매일 전체 교체 | 제휴사 적재 변경 | 큼 | 전송량 | 명단이 작을 때 |
| 월 차집합 대사 | 제휴사 협조 | 중간 | 사후 | 안전망 |

**결론**: 증분 전송은 생성이 아니라 변경을 기준으로, 삭제도 레코드로 보낸다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
