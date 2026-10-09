# issue/unsubreg/channel-scope-sms-only-sync — 정리 (힌트)

태그: `compliance` · `data-semantics`

## 전체 흐름

```
080 대행 벤더 일 파일: phone, channel(SMS|ALIMTALK|ALL), optout_at
unsubreg 동기화: channel 을 읽지 않고 모든 행을 SMS 거부로 저장
  channel=ALIMTALK 행 → SMS 거부로만 기록
광고 알림톡 발송 시 조회: ALIMTALK 거부 없음 → 발송
```

## 핵심 문장

- 벤더는 2025-03부터 채널별 수신거부를 지원해 파일에 `channel` 열을 추가했다.
- 동기화 코드는 열 위치로 읽어 새 열을 무시했고, 모든 거부를 SMS로만 기록했다.
- 알림톡에서만 거부한 고객(약 4,700명)에게 광고 알림톡이 계속 나갔다 — 법적 위험이 있는 사건.
- 대처: 헤더 이름 기반 파싱 + 모르는 열·값이 있으면 동기화 실패, 채널별 거부 저장, 광고 발송 전 ALL·해당 채널 모두 확인.
