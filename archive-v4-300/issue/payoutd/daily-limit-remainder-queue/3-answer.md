# issue/payoutd/daily-limit-remainder-queue — 정답

태그: `partial-processing` · `limit`

## 정답
<!-- 질문 1:1 대응 -->

1. **순서상 뒤쪽 가맹점의 금액이 한도 안 부분만 나가고 잔액은 PENDING_LIMIT로 다음 영업일에 나간다.**

2. **평소 총액이 한도 아래였다.**

3. **아니다.** 전부 늦어진다. 분할은 유지하되 사전 예측과 경보를 둔다.

4. **PENDING_LIMIT 잔액 존재 여부.** 지급 건이 PAID여도 같은 정산의 잔액 건이 따로 있을 수 있다.

## 문제 구조 (추상화 코드)

① 문제 코드
```python
r = payout.run(date)                 # 요약만 기록
```
② 고친 코드
```python
forecast = settle.total(date)
if forecast > bank.daily_limit() * 0.9: alert('limit risk', forecast)
r = payout.run(date)
if r.pending_limit: notify_merchants(r.pending_limit)
```
무엇이 깨졌나: 외부 한도에 의한 분할이 요약 성공에 섞여 보이지 않았다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 사전 예측 + 한도 증액 신청 | 은행 협의 | 예측 오차 | 기본 |
| 대형 가맹점 우선 지급 | 작음 | 소형 가맹점 지연 | 선택하지 않음 |

**결론**: 피크 총액을 미리 예측해 한도를 맞추고, 분할은 경보로 드러낸다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
