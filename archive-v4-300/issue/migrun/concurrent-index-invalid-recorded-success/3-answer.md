# issue/migrun/concurrent-index-invalid-recorded-success — 정답

태그: `silent-failure` · `performance`

## 정답
<!-- 질문 1:1 대응 -->

1. **존재하지만 쓰이지 않는다.** `\d txn`에 `INVALID` 표시가 붙어 있었다.

2. **3.3의 비트랜잭션 문장 오류 처리 결함이다.** 취소 오류가 경고로 내려가 success가 남았다.

3. **`IF NOT EXISTS` 때문에 건너뛴다.** INVALID 인덱스를 `DROP INDEX CONCURRENTLY`로 지운 뒤 다시 만들어야 한다.

4. **`indisvalid` 확인 쿼리를 마이그레이션 뒤 검증 단계로 둔다.** 실패하면 배포를 실패로 올린다.

## 문제 구조 (추상화 코드)

### 변형 A — 인덱스 생성 결과를 러너 기록으로만 판정
① 문제 코드
```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_txn_merchant ON txn(merchant_id, created_at);
```
② 고친 코드
```sql
DROP INDEX CONCURRENTLY IF EXISTS idx_txn_merchant;  -- INVALID 잔존 제거
CREATE INDEX CONCURRENTLY idx_txn_merchant ON txn(merchant_id, created_at);
-- 검증
SELECT indisvalid FROM pg_index WHERE indexrelid = 'idx_txn_merchant'::regclass;  -- true 아니면 실패
```
무엇이 깨졌나: 러너의 success가 DB 상태와 같다고 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| indisvalid 검증 단계 | 검증 SQL | 작음 | 다른 객체 유형은 못 잡음 | 기본 |
| migrun 3.5 업그레이드 | 업그레이드 | 중간 | 업그레이드 전 노출 | 함께 |
| INVALID 인덱스 주간 점검 | 점검 쿼리 | 작음 | 발견 지연 | 안전망 |

**결론**: 인덱스 마이그레이션은 DB 카탈로그로 완료를 확인하고, 러너는 3.5로 올린다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
