# issue/remitx/sanctions-screening-hold — 정리 (힌트)

태그: `async-status` · `compliance`

## 전체 흐름

```
remitx send → 사내 상태 SENT (은행 API 수신 확인)
은행 컴플라이언스: 수취인 이름이 제재 명단과 유사 → 수동 심사 보류
      │  보류는 은행 포털·remitx inquiry 의 compliance 필드에만
      ▼
수취인 미수취 → 문의 → 은행 확인 요청 → 추가 서류 제출 후 송금
```

## 핵심 문장

- `SENT`는 **은행 API가 요청을 받았다**는 뜻이다 — SWIFT 발송이 아니다.
- 심사 보류는 `remitx inquiry <id>`의 `compliance=REVIEW`로만 보인다.
- 이름이 흔한 일부 수취인만 걸렸다.
- 교정: 송금 후 24시간 내 UETR 추적 상태가 진행되지 않으면 조회·경보.
