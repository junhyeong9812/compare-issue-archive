# issue/migrun/add-column-default-rewrite-lock — 정답

태그: `lock` · `deploy`

## 정답
<!-- 질문 1:1 대응 -->

1. **7분간 재작성되며 그동안 읽기·쓰기가 모두 막혔다.** 승인 INSERT가 줄 서다 앱 타임아웃(3초)으로 실패했다.

2. **`0`은 상수라 메타데이터만 바뀐다.** `gen_random_uuid()`는 volatile 함수라 기존 행마다 값을 만들어야 한다.

3. **잠금을 못 얻으면 빨리 실패했을 것이다.** 다만 잠금을 얻은 뒤의 재작성은 못 막는다 — 근본은 문장 분리다.

4. **① 기본값 없이 컬럼 추가 ② 1만 행씩 UPDATE로 채우기 ③ `ALTER ... SET DEFAULT` ④ 필요하면 NOT NULL을 CHECK NOT VALID → VALIDATE 순으로.**

## 문제 구조 (추상화 코드)

### 변형 A — 변동 기본값과 컬럼 추가를 한 문장으로
① 문제 코드
```sql
ALTER TABLE approval ADD COLUMN trace_id uuid DEFAULT gen_random_uuid();
```
② 고친 코드
```sql
SET lock_timeout = '3s';
ALTER TABLE approval ADD COLUMN trace_id uuid;
-- 배치: UPDATE approval SET trace_id = gen_random_uuid() WHERE id BETWEEN ? AND ? AND trace_id IS NULL;
ALTER TABLE approval ALTER COLUMN trace_id SET DEFAULT gen_random_uuid();
```
무엇이 깨졌나: 같은 모양의 DDL이 기본값 종류에 따라 비용이 전혀 다르다는 점을 리뷰가 보지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 문장 분리 + 배치 채우기 | 배치 작업 | 시간 | 배치 중 신규 행 NULL | 기본 |
| 점검 시간에 일괄 실행 | 점검 공지 | 서비스 중단 | 예상보다 길어짐 | 선택하지 않은 방법 |
| migrun 린트로 volatile DEFAULT 차단 | 린트 규칙 | 작음 | 규칙 우회 | 재발 방지 |

**결론**: 문장을 나누고, 마이그레이션 린트에 volatile 기본값 금지를 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
