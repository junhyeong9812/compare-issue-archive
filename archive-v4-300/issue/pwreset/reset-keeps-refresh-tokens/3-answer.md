# issue/pwreset/reset-keeps-refresh-tokens — 정답

태그: `security` · `session`

## 정답
<!-- 질문 1:1 대응 -->

1. **계속 쓸 수 있다.** 리프레시 토큰이 폐기되지 않았다.

2. **다른 세션을 모두 폐기했다.**

3. **재설정 후 새로 로그인해야 한다(재설정 기기도 세션이 없다).**

4. **재설정 기능을 별도 서비스(`pwreset`)로 분리할 때 폐기 호출을 옮기지 않았다.** 2024-12 분리 이후 4개월.

5. **`credentialChanged(user)` 공통 함수에서 비밀번호 저장 + 전체 세션 폐기 + 알림 메일.** 두 경로가 이 함수만 호출한다.

## 문제 구조 (추상화 코드)

### 변형 A — 경로별 구현
① 문제 코드
```java
// pwreset
users.setPassword(userId, newPw);
```
② 고친 코드
```java
// 공통
void credentialChanged(UserId id, String newPw) {
    users.setPassword(id, newPw);
    tokensvc.revokeAll(id);
    notifier.passwordChanged(id);
}
```
무엇이 깨졌나: 보안상 같은 사건(자격증명 변경)을 두 경로가 따로 구현해 한쪽이 빠졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 공통 함수 | 리팩터링 | 작음 | 새 경로 우회 | 기본 |
| 재설정 후 세션 폐기 이벤트 | 이벤트 연동 | 중간 | 이벤트 유실 | 서비스 분리 시 |
| 도용 의심 신고 시 전체 로그아웃 버튼 | UI | 작음 | 사용자 의존 | 보조 |

**결론**: 같은 보안 사건은 한 함수로 처리한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
