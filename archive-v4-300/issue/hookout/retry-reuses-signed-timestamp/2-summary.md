# issue/hookout/retry-reuses-signed-timestamp — 정리 (힌트)

태그: `auth` · `retry`

## 전체 흐름

```
첫 시도 10:00:00  X-Signature = HMAC(ts=10:00:00, body)  → 가맹점 503 (점검)
재시도 10:05, 10:15, 10:35 ... 같은 헤더 재사용
가맹점 SDK: |now - ts| > 300s → 401 "timestamp out of tolerance"
```

## 핵심 문장

- 우리 서명 규격은 `timestamp.body`를 HMAC으로 서명하고, 가맹점 SDK는 **5분 허용 창**으로 재전송 공격을 막는다.
- `hookout`은 서명된 요청 객체를 큐에 저장해 재시도마다 재사용했다 — 첫 재시도부터 창을 넘는다.
- 대처: 재시도마다 타임스탬프·서명을 새로 계산, 이벤트 id는 유지(가맹점 중복 제거용).
