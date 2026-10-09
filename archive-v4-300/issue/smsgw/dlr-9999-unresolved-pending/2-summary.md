# issue/smsgw/dlr-9999-unresolved-pending — 정리 (힌트)

태그: `silent-failure` · `state-mapping`

## 전체 흐름

```
send ──▶ smsgw ──▶ 통신사
                   │ 연휴 트래픽 → 리포트 지연·유실
                   ▼
   24시간 내 리포트 없음 → smsgw 가 report=9999(결과 미수신) 로 확정 DLR 발송
   우리 상태 매핑:  0000 → DELIVERED / 3xxx·4xxx → FAILED / 그 외 → 상태 유지(SENDING)
                                                       ▲ 9999 가 여기에 떨어짐
   재발송 배치: FAILED 만 대상 → SENDING 건은 영원히 제외
```

## 핵심 문장

- `smsgw`는 통신사 리포트를 24시간 기다린 뒤 **`9999`(결과 미수신)** 로 확정한다 — 실패도 성공도 아닌 "모름"이다.
- 우리 매핑은 모르는 코드를 "상태 유지"로 처리해 해당 건이 `SENDING`에 영원히 머물렀다.
- 재발송 배치가 `FAILED`만 보므로 "모름" 건은 재발송 대상에서 빠졌다.
- 대처: `9999`를 별도 상태 `UNKNOWN`으로 두고, 업무 중요도에 따라 재발송 또는 다른 채널 안내.
