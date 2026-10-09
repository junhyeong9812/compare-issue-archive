# issue/billkey/card-reissue-billing-key-invalid — 정답

태그: `external-dependency` · `lifecycle`

## 정답
<!-- 질문 1:1 대응 -->

1. **5년 전 같은 시기에 가입한 회원들의 카드 유효기간이 한꺼번에 끝났다.**

2. **아니다.** 카드 자체가 바뀌어 재시도는 계속 거절된다.

3. **잔액 부족(재시도 의미 있음)·유효기간/분실(재등록 필요)·한도 초과(다음 날 재시도)로 나눈다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```python
for _ in range(3): if billkey.charge(k, amt).ok: break
```
② 고친 코드
```python
r = billkey.charge(k, amt)
if r.reason in ('EXPIRED', 'LOST'): notify_reregister(member)
elif r.reason == 'INSUFFICIENT': schedule_retry(member, days=[1, 3])
```
무엇이 깨졌나: 외부 카드 생애주기를 모델에 넣지 않았다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 자동 갱신 가입 + 사유별 대응 | PG 수수료 | 갱신 미지원 카드사 | 기본 |
| 재시도 증가 | 작음 | 효과 없음 | 선택하지 않음 |

**결론**: 빌링키는 카드 생애주기와 함께 관리한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
