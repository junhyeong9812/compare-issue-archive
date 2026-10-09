# issue/replwatch/inactive-slot-wal-disk-full — 정리 (힌트)

태그: `capacity` · `silent-failure`

## 전체 흐름

```
cdc_ledger 슬롯 (active=f, restart_lsn 고정 11일 전)
   └ 그 이후 WAL 전부 보존 → pg_wal 410GB
야간 대량 정산 적재 → WAL 생성 급증 → 80% → 100% (3시간)
→ 쓰기 불가 → 승인 실패
```

## 핵심 문장

- 복제 슬롯은 **소비자가 읽은 위치까지 WAL을 지우지 못하게** 한다 — 소비자가 사라져도 슬롯은 남는다.
- `replwatch slots`에서 `active=f`인데 `retained` 크기가 계속 크는 슬롯이 범인이다.
- `max_slot_wal_keep_size`는 상한을 두지만, 넘으면 그 슬롯은 **무효화**돼 소비자가 처음부터 다시 받아야 한다.
- 커넥터 폐기 절차에 슬롯 삭제를, 모니터링에 비활성 슬롯 보존량 경보를 둔다.
