# issue/cashrcpt/sigterm-inflight-duplicate-issue — 정리 (힌트)

태그: `deploy` · `idempotency`

## 전체 흐름

```
발행 소비자: 메시지 수신 → 대행사 발행 호출 → 성공 → ack
배포: SIGTERM → 즉시 종료 (graceful 없음)
      │  발행은 됐는데 ack 전 종료 → 메시지 재전달
      ▼
새 파드가 같은 메시지 처리 → 대행사 '중복 요청' 거절 (요청 키 동일)
```

## 핵심 문장

- 대행사의 **요청 키 중복 거절** 덕분에 이중 발급은 없었다.
- 사내 소비자는 중복 거절을 오류로 기록해 경보가 터졌다.
- 교정: graceful shutdown(처리 중 메시지 완료 후 종료), 중복 거절은 성공으로 취급.
