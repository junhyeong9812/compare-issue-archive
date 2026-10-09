# issue/filegw/relay-cert-reload-retry-expire — 정리 (힌트)

태그: `silent-failure` · `environment-drift` · `fail-closed`

## 전체 흐름

```
정산 배치 ──put──▶ filegw 스풀 ──(릴레이 워커)──mTLS──▶ 파트너 은행
            │         │                    │
            │         │ st Q (queued)      │ 핸드셰이크 실패 (인증서 만료)
            │         │                    ▼
            │         │                 st R (retry) ── 15분 간격 재시도
            │         │                    │
            │         │                 72시간 경과
            │         │                    ▼
            │         │                 st X (expired) → spool/dead/ 로 이동
            ▼
   stdout "queued F000xxxxx", exit 0   ← 배치는 여기서 "전송 완료" 기록

인증서 교체 함정
  키스토어 파일 교체  ──X──▶ 워커는 기동 시 읽은 인증서를 메모리에 계속 사용
  filegw reload       ──O──▶ 새 인증서 적재 (tls-check 로 확인)
```

## 핵심 문장

- `filegw put`의 `queued`는 **스풀 적재**일 뿐 파트너 전달이 아니다 — 전달 완료는 `status`의 `D`다.
- 릴레이 핸드셰이크 실패는 오류가 아니라 **`R`(재시도)** 상태로 72시간 머문 뒤 `X`로 끝난다 — 그 사이 경보는 기본값으로 꺼져 있다.
- 릴레이 워커는 인증서를 **기동 시 한 번** 읽는다 — 파일 교체만으로는 반영되지 않고 `filegw reload`가 필요하다.
- `X` 파일은 `spool/dead/`에 남고 자동 재전송되지 않는다 — `filegw requeue <id>`로 다시 넣는다.
- 배치는 `put` 뒤 `D`까지 확인하고, 인증서는 교체 후 `tls-check`로 실제 핸드셰이크를 본다.
