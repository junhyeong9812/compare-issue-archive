# issue/oidcbr/frontchannel-logout-3p-cookie — 정리 (힌트)

태그: `browser` · `session`

## 전체 흐름

```
pay.hanbit… 로그아웃 페이지
  <iframe src="https://mall.partner…/logout"> → 제휴몰 입장에서 서드파티 컨텍스트
  서드파티 쿠키 차단 → 세션 쿠키 안 실림 → 제휴몰은 누구를 로그아웃할지 모름
교정: back-channel logout (서버 간 호출, sid 기반)
```

## 핵심 문장

- front-channel 로그아웃은 iframe 안에서 **제휴몰 쿠키가 실려야** 동작한다.
- 서드파티 쿠키 차단이 기본이 되면서 **제휴몰 세션이 남았다**.
- 공용 PC에서 다음 사용자가 제휴몰 세션을 이어 쓸 수 있다.
- **back-channel 로그아웃**(서버 간, 세션 ID 기반)으로 바꾼다.
