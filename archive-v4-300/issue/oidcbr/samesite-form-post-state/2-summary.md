# issue/oidcbr/samesite-form-post-state — 정리 (힌트)

태그: `browser` · `security`

## 전체 흐름

```
oidcbr ──redirect──▶ IdP B ──(form_post: 크로스 사이트 POST)──▶ /callback
  쿠키 state (SameSite=Lax) → 크로스 사이트 POST 에 미전송
  → state 불일치 → 로그인 화면으로
다른 IdP: query 모드(GET 리다이렉트) → Lax 쿠키 전송됨
교정: state 쿠키만 SameSite=None; Secure
```

## 핵심 문장

- `SameSite=Lax` 쿠키는 **다른 사이트에서 오는 POST**에는 실리지 않는다.
- IdP B만 **form_post** 응답 모드라 실패했다 — GET 리다이렉트는 정상.
- SameSite 기본값이 Lax인 브라우저(사파리·최신 크롬)에서만 터졌다.
- state 쿠키만 `SameSite=None; Secure`로 두고 짧은 수명으로 한다.
