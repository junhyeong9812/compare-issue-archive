# issue/payoutd/approver-equals-requester — 정답

태그: `security` · `authorization`

## 정답
<!-- 질문 1:1 대응 -->

1. **API가 역할만 검사하고 요청자와의 관계를 검사하지 않았다.**

2. **다른 승인자의 승인을 받는 긴급 경로를 두고, 야간 당번 승인자를 지정했다.** 단독 실행 경로는 없다.

3. **전 건을 사후 검토해 정상 지급임을 확인하고 감사에 보고했다.** 금액 손실은 없었다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
@PreAuthorize("hasRole('PAYOUT_APPROVER')")
public void approve(long id, User u) { manual.approve(id, u); }
```
② 고친 코드
```java
@PreAuthorize("hasRole('PAYOUT_APPROVER')")
public void approve(long id, User u) {
  var req = manual.get(id);
  if (req.requestedBy().equals(u.id())) throw new Forbidden("self-approval");
  manual.approve(id, u);
}
```
무엇이 깨졌나: 통제 규칙을 클라이언트에만 구현했다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 서버 검사 + 감사 로그 | 작음 | 없음 | 기본 |
| 화면 통제 유지 | 없음 | API 직접 호출 | 선택하지 않음 |

**결론**: 권한 통제는 서버에서 강제한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
