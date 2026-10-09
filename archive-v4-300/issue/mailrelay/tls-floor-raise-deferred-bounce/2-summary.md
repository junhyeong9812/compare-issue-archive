# issue/mailrelay/tls-floor-raise-deferred-bounce — 정리 (힌트)

태그: `environment-drift` · `compatibility`

## 전체 흐름

```
보안 점검 → mailrelay smtp_tls_mandatory_protocols = >=TLSv1.2 , 암호화 필수
수신 서버 corp-a.example (TLS 1.0 만 지원)
   handshake 실패 → 4.7.x deferred → 재시도(1h, 2h, 4h ...) → 5일 후 bounce
발송 API: 202 (큐 적재) / 서비스 로그 정상
```

## 핵심 문장

- 보안 기준으로 TLS 하한과 **암호화 필수**를 함께 켜자, TLS 1.0만 지원하는 수신 서버와는 연결이 성립하지 않았다.
- 실패는 `deferred`(일시 실패)로 분류돼 5일 동안 재시도 큐에 머문 뒤에야 반송됐다.
- 영향 도메인은 기업 가맹점 7곳, 메일 약 3,800통.
- 대처: 수신 도메인별 정책 맵으로 예외 도메인은 기회적 TLS로 두고, 해당 가맹점에 서버 갱신을 요청했다.
