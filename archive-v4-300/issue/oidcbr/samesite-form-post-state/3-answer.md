# issue/oidcbr/samesite-form-post-state — 정답

태그: `browser` · `security`

## 정답
<!-- 질문 1:1 대응 -->

1. **오지 않는다.** 크로스 사이트 POST라 Lax 쿠키가 빠진다.

2. **다른 IdP는 GET 리다이렉트(query 모드)였다.** Lax는 최상위 GET 이동에는 쿠키를 보낸다.

3. **`Secure`(HTTPS)가 필요하다.** 일부 옛 사파리는 None을 잘못 처리해 별도 대비 쿠키를 둔다.

4. **옛 브라우저는 SameSite 속성이 없으면 제한을 두지 않았다.**

5. **state 쿠키는 `SameSite=None; Secure; Max-Age=600`, 세션 쿠키는 Lax 유지.** 옛 사파리용으로 속성 없는 대비 쿠키를 함께 둔다.

## 문제 구조 (추상화 코드)

### 변형 A — Lax state 쿠키
① 문제 코드
```java
ResponseCookie.from("oidc_state", state).sameSite("Lax").httpOnly(true).build();
```
② 고친 코드
```java
ResponseCookie.from("oidc_state", state)
    .sameSite("None").secure(true).httpOnly(true).maxAge(600).build();
```
무엇이 깨졌나: 브라우저 쿠키 정책과 IdP 응답 방식의 조합을 시험하지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| state 쿠키 None+Secure | HTTPS | 작음 | 옛 사파리 | 기본 |
| query 모드로 변경 | IdP 지원 | 작음 | 토큰이 URL에 | IdP 지원 시 |
| state 를 서버 세션에 | 세션 저장소 | 중간 | 세션 식별 쿠키 같은 문제 | 보조 |

**결론**: 크로스 사이트로 돌아오는 흐름의 쿠키는 그 흐름에 맞는 SameSite를 따로 정한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
