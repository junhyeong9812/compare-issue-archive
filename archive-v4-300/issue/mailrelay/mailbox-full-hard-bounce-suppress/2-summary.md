# issue/mailrelay/mailbox-full-hard-bounce-suppress — 정리 (힌트)

태그: `silent-failure` · `classification`

## 전체 흐름

```
mailrelay ──▶ 수신 서버: 552 5.2.2 mailbox full
       반송 처리기: 5xx → hard bounce → suppression 목록 영구 등록
이후 발송 요청 ──▶ mailrelay API 202 Accepted
                  └─ suppression 목록에 있음 → 전송 없이 drop (로그 level=INFO "suppressed")
고객이 메일함을 비운 뒤에도 해제 경로 없음
```

## 핵심 문장

- `mailrelay` 반송 처리기는 응답 코드 **첫 자리 5면 영구 반송**으로 보고 주소를 suppression 목록에 넣었다.
- `552 5.2.2`(메일함 가득)는 확장 코드상 **일시적 상태**다 — 고객이 메일함을 비우면 받을 수 있다.
- suppression 주소로 가는 요청도 API는 `202`를 주고 조용히 버린다 — 서비스 쪽에서는 보이지 않는다.
- 대처: 분류 기준을 확장 코드(`5.2.2`→soft)로 바꾸고, soft 반송은 3회·7일 누적 시에만 일시 차단(30일 후 자동 해제).
