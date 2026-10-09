# issue/oidcbr/frontchannel-logout-3p-cookie — 정답

태그: `browser` · `session`

## 정답
<!-- 질문 1:1 대응 -->

1. **제휴몰 세션이 지워지지 않는다.** iframe 요청에 쿠키가 실리지 않는다.

2. **당시 주요 브라우저가 서드파티 쿠키를 기본 허용했다.**

3. **같은 사이트(상위 도메인 동일)면 퍼스트파티로 취급돼 동작한다.** 제휴몰은 다른 도메인이었다.

4. **다음 사용자가 제휴몰 탭에서 이전 사용자의 주문·주소를 볼 수 있다.** 보안 점검에서 재현됐다.

5. **oidcbr이 로그아웃 시 제휴몰에 `logout_token`(sid 포함)을 서버 간 POST.** 제휴몰은 sid로 세션을 지운다. 제휴몰 3곳과 일정 합의 후 전환.

## 문제 구조 (추상화 코드)

### 변형 A — iframe 로그아웃
① 문제 코드
```html
<iframe src="https://mall.partner.example/logout" hidden></iframe>
```
② 고친 코드
```html
<!-- 브라우저 경로 제거. 서버에서 -->
POST https://mall.partner.example/backchannel-logout
logout_token=<JWT {sid, events: back-channel-logout}>
```
무엇이 깨졌나: 브라우저 쿠키 정책 변화에 기대는 흐름을 그대로 두었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| back-channel | 제휴몰 구현 | 중간 | 제휴몰 미구현 | 기본 |
| 제휴몰 세션 수명 단축 | 제휴몰 설정 | 작음 | UX | 보조 |
| 로그아웃 시 제휴몰 탭 안내 | UI | 작음 | 사용자 의존 | 임시 |

**결론**: 세션 종료는 브라우저가 아닌 서버 간 통보로 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
