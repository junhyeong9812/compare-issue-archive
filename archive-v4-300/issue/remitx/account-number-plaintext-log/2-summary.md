# issue/remitx/account-number-plaintext-log — 정리 (힌트)

태그: `security` · `privacy`

## 전체 흐름

```
은행 API 오류 응답: 요청 본문을 에코 (IBAN·이름 포함)
remitx: log.warn("bank error: " + responseBody)
      ▼
로그 저장소 90일 보관
```

## 핵심 문장

- 사내 코드는 계좌번호를 직접 찍지 않았지만 **상대 응답이 요청을 에코**했다.
- 은행 API 버전 업그레이드(2023-07)부터 오류 응답이 요청 본문을 에코하기 시작했다 — 사내 코드 변경은 없었다.
- 교정: 외부 응답 본문은 오류 코드·메시지 필드만 추출해 기록, 로그 출력 필터에 IBAN 패턴 추가.
