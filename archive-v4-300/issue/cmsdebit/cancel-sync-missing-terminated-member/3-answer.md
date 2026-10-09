# issue/cmsdebit/cancel-sync-missing-terminated-member — 정답

태그: `data-sync` · `customer-impact`

## 정답
<!-- 질문 1:1 대응 -->

1. **모바일 경로만 CMS 해지를 호출했다.**

2. **전액 환불하고 해당 회원을 CMS에서 해지했다.**

3. **한 시스템의 상태만 믿으면 동기화 누락이 곧 오출금이 된다.** 두 상태의 교집합만 출금한다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
member.setStatus(TERMINATED);           // 고객센터 경로
```
② 고친 코드
```java
membershipService.terminate(memberId);  // 사내 상태 + cms.memberDel 을 한 곳에서
// 출금 대상: cmsRegistered ∩ activeMembers
```
무엇이 깨졌나: 같은 업무 동작이 경로마다 다르게 구현됐다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 해지 단일 진입점 + 교집합 선정 | 중간 | 없음 | 기본 |
| 야간 동기화 배치 | 작음 | 하루 지연 | 보조 |

**결론**: 업무 동작은 한 곳에서 하고, 돈이 나가는 대상은 양쪽 상태로 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
