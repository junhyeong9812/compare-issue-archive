# issue/pwreset/mail-scanner-consumes-token — 정리 (힌트)

태그: `idempotency` · `security`

## 전체 흐름

```
메일 도착 → 보안 스캐너 GET /reset?token=T  → 토큰 사용 처리 (스캐너 IP)
사용자 클릭 GET /reset?token=T → 이미 사용 → '만료된 링크'
교정: GET = 확인 화면만 (토큰 소비 안 함)
      POST /reset {token, new_pw} = 토큰 소비 + 비밀번호 변경
```

## 핵심 문장

- **GET에서 토큰을 소비**해, 링크를 미리 여는 메일 스캐너가 토큰을 써 버렸다.
- 법인 메일(보안 게이트웨이 사용)만 영향이 있었다.
- 유효 시간과 무관하다 — 사용 처리가 문제다.
- GET은 부작용 없이 화면만, 토큰 소비는 **POST(비밀번호 제출)**에서 한다.
