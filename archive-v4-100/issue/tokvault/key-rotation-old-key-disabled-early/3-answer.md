# issue/tokvault/key-rotation-old-key-disabled-early — 정답

태그: `security` · `key-management`

## 정답
<!-- 질문 1:1 대응 -->

1. **재암호화가 최신 행부터 돌아 오래된 행이 구 키로 남아 있었다.**

2. **복구 불가능한 데이터 손실이다.** 4.1만 회원 카드 재등록이 필요했을 것이다.

3. **구 키 사용 행 수 0 확인을 명령 자체의 전제 조건으로 넣었다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```java
kms.disable(oldKeyId);
```
② 고친 코드
```java
long remain = repo.countByKeyId(oldKeyId);
if (remain > 0) throw new IllegalStateException("rows still on old key: " + remain);
kms.disable(oldKeyId);
```
무엇이 깨졌나: 되돌리기 어려운 조작을 진행 상태 확인 없이 실행했다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 전제 조건 검사 내장 | 작음 | 없음 | 기본 |
| 절차 문서만 | 없음 | 사람 실수 | 선택하지 않음 |

**결론**: 키 비활성화는 남은 사용처 0을 기계적으로 확인한 뒤에만 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
