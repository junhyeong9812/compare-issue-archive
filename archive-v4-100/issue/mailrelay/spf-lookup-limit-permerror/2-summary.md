# issue/mailrelay/spf-lookup-limit-permerror — 정리 (힌트)

태그: `environment-drift` · `silent-failure`

## 전체 흐름

```
hanbitpay.co.kr TXT "v=spf1 include:_spf.relay include:crm.vendor include:survey.vendor
                     include:helpdesk.vendor include:ads.vendor ... ~all"
   중첩 include 를 펼치면 DNS 조회 13회
       │
수신 서버 X (엄격): 10회 초과 → permerror → SPF fail 취급 → 스팸함
수신 서버 Y (관대): 처음 10회 안에서 매칭되면 pass → 정상
mailrelay 로그: status=sent (250 OK)   ← 수신 서버는 받았다
```

## 핵심 문장

- SPF 평가는 **DNS 조회 10회**가 상한이다 — 넘으면 `permerror`이고, 엄격한 수신 서버는 이를 실패로 본다.
- 3월에 설문 벤더 include를 추가하면서 중첩 조회가 13회가 됐다 — DNS 변경 검토에 조회 수 계산이 없었다.
- 수신 서버는 메일을 **받은 뒤** 스팸함에 넣으므로 릴레이 로그는 `250 OK`다.
- 대처: 쓰지 않는 include 제거, 벤더 발송은 하위 도메인(`mail.`, `crm.`)으로 분리해 조회 수를 줄였다.
