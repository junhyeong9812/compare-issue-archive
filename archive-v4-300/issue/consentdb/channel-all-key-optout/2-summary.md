# issue/consentdb/channel-all-key-optout — 정리 (힌트)

태그: `data-semantics` · `privacy`

## 전체 흐름

```
consentdb 키
  mkt.ALL   ← 가입 화면 일괄 동의/철회
  mkt.SMS / PUSH / EMAIL   ← 2024-07 앱 설정 화면(채널별)
앱 설정에서 SMS 끔 → mkt.SMS = N, ALL = Y 그대로
광고 발송: ALL == Y ? → 발송   ← 채널 키를 보지 않음
올바른 판정: ALL == Y AND <채널> != N
```

## 핵심 문장

- 2024-07 앱 설정에 **채널별 철회**가 생겼지만 발송 서비스는 **ALL 키만** 봤다.
- 가입 화면 철회는 ALL을 바꿔 정상이었다 — 앱 설정에서 끈 고객만 계속 받았다.
- 동의 판정은 **consentdb의 판정 API 한 곳**에 두고, 발송 쪽은 키를 직접 해석하지 않는다.
- 발송 직전 판정 결과를 발송 로그에 남겨 민원 조사 시 근거로 쓴다.
