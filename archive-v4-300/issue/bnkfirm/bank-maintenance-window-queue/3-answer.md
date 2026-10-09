# issue/bnkfirm/bank-maintenance-window-queue — 정답

태그: `async-status` · `external-dependency`

## 정답
<!-- 질문 1:1 대응 -->

1. **응답은 ACCEPTED/QUEUED_MAINT, 입금은 점검 종료 직후다.**

2. **수취 은행이 그 시각 점검 중인 가맹점만 늦었다.**

3. **불가능하다.** 입금은 수취 은행이 해야 한다. 안내가 해법이다.

## 문제 구조 (추상화 코드)

① 문제 코드
```python
r = firm.transfer(p); payout.set_paid(p.id) if r.result == 'ACCEPTED' else ...
```
② 고친 코드
```python
r = firm.transfer(p)
payout.set_state(p.id, {'DONE': 'PAID', 'QUEUED_MAINT': 'DELAYED_BANK'}.get(r.st, 'SENDING'))
```
무엇이 깨졌나: 접수 응답의 세부 상태를 버리고 완료로 표시했다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 세부 상태 반영 + 안내 | 작음 | 없음 | 기본 |
| 점검 시간 지급 중단 | 작음 | 다른 은행 가맹점까지 지연 | 선택하지 않음 |

**결론**: 접수 응답의 세부 상태를 화면과 완료 판정에 그대로 쓴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
