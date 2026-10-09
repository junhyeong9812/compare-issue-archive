# issue/pwreset/mail-scanner-consumes-token — 정답

태그: `idempotency` · `security`

## 정답
<!-- 질문 1:1 대응 -->

1. **'만료된 링크'가 뜬다.** 스캐너가 먼저 열어 토큰이 사용 처리됐다.

2. **개인 메일 서비스는 링크를 미리 열지 않았다.**

3. **안 된다.** 스캐너는 몇 초 안에 연다.

4. **토큰 사용 IP가 메일 보안 업체 대역이고, 사용 시각이 발송 후 2~5초였다.** 사용자 클릭은 그 뒤였다.

5. **GET은 토큰 유효성만 확인하고 입력 화면을 띄운다. 토큰 소비는 새 비밀번호 POST에서 한다.** 고객 문의 9월 한 달 120여 건이 그 뒤 사라졌다.

## 문제 구조 (추상화 코드)

### 변형 A — GET에서 토큰 소비
① 문제 코드
```java
@GetMapping("/reset")
String show(@RequestParam String token) {
    tokens.consume(token);            // 부작용
    return "new-password";
}
```
② 고친 코드
```java
@GetMapping("/reset")
String show(@RequestParam String token) {
    tokens.requireValid(token);       // 읽기만
    return "new-password";
}
@PostMapping("/reset")
String submit(@RequestParam String token, @RequestParam String pw) {
    tokens.consume(token); users.changePassword(token, pw); return "done";
}
```
무엇이 깨졌나: 안전해야 할 GET 요청에 상태 변경을 걸어, 사람이 아닌 방문자가 상태를 바꿨다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| GET 읽기 · POST 소비 | 코드 수정 | 작음 | 없음 | 기본 |
| 스캐너 IP 무시 | IP 목록 | 작음 | 목록 갱신 | 선택하지 않은 방법 |
| 토큰 다회 사용 허용 | 정책 변경 | 작음 | 보안 약화 | 선택하지 않은 방법 |

**결론**: GET은 부작용 없게. 일회성 자원 소비는 사람의 명시 행동에 묶는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
