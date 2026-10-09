# issue/migrun/concurrent-index-invalid-recorded-success — 정리 (힌트)

태그: `silent-failure` · `performance`

## 전체 흐름

```
CREATE INDEX CONCURRENTLY  (non-transactional 모드)
   ├ 카탈로그에 인덱스 등록 (indisvalid=false)
   ├ 2차 스캔 중 deadlock → 취소
   └ migrun: 문장 실행 '종료' → success 기록 (오류 코드 처리 누락 버전)
결과: INVALID 인덱스 — 플래너가 쓰지 않음, 쓰기 비용만 발생
```

## 핵심 문장

- `CONCURRENTLY`가 실패하면 인덱스는 **INVALID로 남는다** — 플래너는 쓰지 않지만 쓰기 때마다 갱신 비용은 든다.
- `migrun` 3.3은 비트랜잭션 모드 문장의 일부 오류를 경고로만 처리해 **success로 기록**했다(3.5에서 수정).
- `IF NOT EXISTS`로 다시 돌리면 이미 있다고 보고 **건너뛴다**.
- 완료 판정은 `pg_index.indisvalid = true` 확인으로 한다.
