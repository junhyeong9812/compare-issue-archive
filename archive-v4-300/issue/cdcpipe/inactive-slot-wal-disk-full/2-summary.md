# issue/cdcpipe/inactive-slot-wal-disk-full — 정리 (힌트)

태그: `infra` · `capacity`

## 전체 흐름

```
검증용 커넥터 cdc_exp 중지 (슬롯은 남김)
슬롯 restart_lsn 고정 → 그 이후 WAL 삭제 불가
WAL 증가 하루 약 120GB → 디스크 98% → 쓰기 중단 직전
```

## 핵심 문장

- 논리 복제 슬롯은 소비자가 읽지 않은 WAL을 **무기한** 보존한다.
- 커넥터 정지 = 슬롯 비활성이지만 슬롯은 계속 WAL을 붙잡는다.
- 대처: 슬롯 삭제(검증용이라 데이터 손실 없음), `max_slot_wal_keep_size` 설정, 슬롯 지연 경보.
