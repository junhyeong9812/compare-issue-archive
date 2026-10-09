# issue/oidcbr/samesite-form-post-state — 응답 모드 form_post에서 SameSite=Lax 상태 쿠키가 전송되지 않아 state 검증이 실패한다 — 질문

## 질문
1. (예측) oidcbr은 로그인 시작 때 `state`를 `SameSite=Lax` 쿠키에 넣는다. IdP B는 결과를 `response_mode=form_post`(다른 사이트에서 POST)로 보낸다. 콜백에서 쿠키는 오는가?
2. (왜) 다른 IdP는 왜 괜찮았나?
3. (경계) `SameSite=None`으로 바꾸면 무엇이 필요한가?
4. (왜) 왜 옛 브라우저에서는 됐나?
5. (연결) 어떻게 고치나?

## 복습 기록
| 날짜 | 결과 | 틀린 질문 | 다음 복습 |
|------|------|-----------|-----------|
| | | | |
