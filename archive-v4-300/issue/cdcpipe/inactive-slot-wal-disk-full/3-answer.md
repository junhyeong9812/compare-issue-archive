# issue/cdcpipe/inactive-slot-wal-disk-full — 정답

태그: `infra` · `capacity`

## 정답
<!-- 질문 1:1 대응 -->

1. **슬롯이 남아 있는 한 계속 쌓인다.**

2. **경보 임계가 95%였고 증가 속도 경보는 없었다.**

3. **한도를 넘으면 슬롯이 무효가 되어 그 소비자는 재스냅샷이 필요하다** — 디스크를 지키는 대신 소비자 연속성을 포기한다.

## 문제 구조 (추상화 코드)

### 변형 A — 슬롯 상한·감시 없음
① 문제 코드
```sql
-- postgresql.conf 기본값 (max_slot_wal_keep_size = -1, 무제한)
```
② 고친 코드
```sql
ALTER SYSTEM SET max_slot_wal_keep_size = '200GB';
SELECT slot_name, active, pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn))
  FROM pg_replication_slots;            -- 30GB 초과 시 경보
```
무엇이 깨졌나: 소비자 수명과 슬롯 수명을 따로 관리했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| WAL 보존 상한 | 소비자 재스냅샷 감수 | 작음 | 소비자 끊김 | 기본 |
| 슬롯 지연 경보 | 모니터링 | 작음 | 없음 | 항상 |

**결론**: 슬롯은 소비자와 함께 만들고 지우며, WAL 보존에 상한과 경보를 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
