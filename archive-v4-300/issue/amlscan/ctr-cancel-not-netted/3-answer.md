# issue/amlscan/ctr-cancel-not-netted — 정답

태그: `data-semantics` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **잡힌다(잘못).** amlscan은 세 행을 모두 더해 1,600만 원으로 본다. 취소를 빼면 800만 원으로 대상이 아니다.

2. **회계 집계는 type에 따라 부호를 붙였기 때문이다.** 같은 피드라도 해석 규칙이 달랐다. amlscan의 집계 입력 명세는 '금액 열을 합산'으로만 적혀 있었다.

3. **원래 거래일 합계는 그대로다.** 다음 날 취소는 다음 날 행으로 들어오며, 이 경우 원 거래일은 실제로 대상일 수 있다(취소 전 시점 기준). 이번 사건은 같은 날 취소만 문제였다.

4. **정정 보고와 개인정보 과다 제공 때문이다.** 3개월 114건을 정정했고, 내부 감사에서 '보고 대상 판정 근거 불명확' 지적을 받았다.

5. **어느 고객이 틀렸는지 못 본다.** 건수 차이만 보면 '집계 시점 차이'로 넘기기 쉽다. 고객별 합계를 맞대면 취소가 있는 고객만 어긋나는 패턴이 바로 보인다.

## 문제 구조 (추상화 코드)

### 변형 A — type 없이 합산
① 문제 코드
```sql
SELECT customer_id, SUM(amount) AS total
FROM cash_feed WHERE biz_day = :d
GROUP BY customer_id HAVING SUM(amount) >= 10000000;
```
② 고친 코드
```sql
SELECT customer_id,
       SUM(CASE type WHEN 'REV' THEN -amount ELSE amount END) AS total
FROM cash_feed WHERE biz_day = :d
GROUP BY customer_id
HAVING SUM(CASE type WHEN 'REV' THEN -amount ELSE amount END) >= 10000000;
```
무엇이 깨졌나: 취소를 type으로만 표시하는 피드를 금액 합산으로 읽었다 — 보고 판정과 회계 집계가 같은 데이터를 다르게 해석했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 집계 전 취소 상계(전처리) | type 열 신뢰 | 작음 | type 누락 행 | 기본 |
| 원천 피드에 음수 금액 요구 | 원천팀 변경 | 큼 | 다른 소비자 영향 | 선택하지 않은 방법 |
| 고객별 합계 월 대사 | 회계 집계 접근 | 작음 | 사후 발견 | 안전망 |

**결론**: 부호는 원천 행의 type으로 정하고, 보고 판정과 회계 집계가 같은 해석을 쓰는지 고객별 금액으로 대사한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
