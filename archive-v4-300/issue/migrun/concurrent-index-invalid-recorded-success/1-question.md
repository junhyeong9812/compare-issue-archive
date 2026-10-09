# issue/migrun/concurrent-index-invalid-recorded-success — CONCURRENTLY 인덱스 생성이 중간에 실패하면 INVALID 인덱스가 남고 러너는 성공으로 기록한다 — 질문

## 질문
1. (예측) `CREATE INDEX CONCURRENTLY idx_txn_merchant ...`가 생성 중 데드락으로 취소됐다. 인덱스는 존재하는가? 쓰이는가?
2. (왜) `migrun history`는 이 마이그레이션을 success로 보였다. 왜?
3. (경계) 같은 마이그레이션을 다시 돌리면?
4. (연결) 인덱스 마이그레이션 완료를 무엇으로 판정해야 하나?

## 복습 기록
| 날짜 | 결과 | 틀린 질문 | 다음 복습 |
|------|------|-----------|-----------|
| | | | |
