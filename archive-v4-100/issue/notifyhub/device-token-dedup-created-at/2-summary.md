# issue/notifyhub/device-token-dedup-created-at — 정리 (힌트)

태그: `silent-failure` · `data-semantics`

## 전체 흐름

```
회원 M 토큰 테이블
  t1 (created 2023-03, updated 2024-08-20 재등록 — 같은 단말 id, 새 토큰 값으로 UPDATE)
  t2 (created 2024-05, updated 2024-05 — 예전 태블릿, 앱 삭제됨)
야간 정리: 회원당 1개만 유지 = ORDER BY created_at DESC LIMIT 1 → t2 남김, t1 삭제
발송: t2 로 전송 → 플랫폼 응답 200 (만료 토큰 판정은 수 일 뒤) → notifyhub "성공"
```

## 핵심 문장

- 재설치 시 클라이언트는 **같은 단말 id 행을 UPDATE**한다 — 생성 시각은 옛날 그대로, 갱신 시각만 새로 바뀐다.
- 정리 작업이 `created_at`으로 최신을 골라, 실제로 쓰는 토큰을 지우고 안 쓰는 토큰을 남겼다.
- 지워진 토큰이 없는 단말로 보내도 플랫폼이 즉시 실패를 주지 않아 발송은 "성공"이었다.
- 대처: 정리 기준을 `updated_at`(마지막 등록)으로 바꾸고, 회원당 단말 3개까지 유지하도록 정책 변경.
