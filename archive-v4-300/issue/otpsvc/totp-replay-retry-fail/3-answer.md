# issue/otpsvc/totp-replay-retry-fail — 정답

태그: `idempotency` · `security`

## 정답
<!-- 질문 1:1 대응 -->

1. **401 '잘못된 코드'.** 첫 요청이 그 창의 코드를 이미 사용 처리했다.

2. **가로챈 코드를 같은 30초 안에 다시 쓰는 공격을 막기 위해서다.** 끄면 안 된다.

3. **다른 시간 창의 코드라 사용 기록이 없다.**

4. **메시지가 '잘못된 코드'였다.** 실제로는 '이미 사용된 코드'였다. 사용자 문의 6주간 40여 건.

5. **`req_id`로 멱등 처리.** 같은 `req_id`로 들어온 재시도는 2분 안이면 첫 결과(세션 토큰)를 돌려준다. 메시지도 '이미 사용된 코드'로 나눴다.

## 문제 구조 (추상화 코드)

### 변형 A — 재시도와 재사용 구분 없음
① 문제 코드
```java
if (used.contains(user, window)) return unauthorized("invalid code");
```
② 고친 코드
```java
Result prev = idem.get(req.id());
if (prev != null) return prev;                     // 같은 요청 재시도
if (used.contains(user, window)) return unauthorized("code already used");
Result r = verifyAndIssue(req); idem.put(req.id(), r, Duration.ofMinutes(2));
return r;
```
무엇이 깨졌나: 보안 규칙(재사용 거부)이 같은 요청의 재전송까지 공격으로 취급했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| req_id 멱등 | 앱 수정 | 작음 | req_id 재사용 악용 | 기본 |
| 앱 자동 재시도 끄기 | 앱 수정 | 작음 | 망 불안정 시 실패 | 보조 |
| 재사용 거부 해제 | 없음 | 작음 | 보안 약화 | 선택하지 않은 방법 |

**결론**: 재사용 거부와 재시도는 요청 식별자로 구분한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
