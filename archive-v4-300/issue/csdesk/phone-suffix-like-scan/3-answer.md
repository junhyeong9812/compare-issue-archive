# issue/csdesk/phone-suffix-like-scan — 정답

태그: `performance` · `database`

## 정답
<!-- 질문 1:1 대응 -->

1. **전체 테이블 스캔.** 앞 와일드카드라 인덱스를 쓰지 못하고 1,900만 행을 읽는다.

2. **시험 DB가 2만 행이었다.** 스캔해도 30ms였다.

3. **없다.** 접두 일치는 인덱스 범위 스캔이 된다.

4. **상담 콜이 몰려 같은 쿼리가 초당 40건 동시에 돌았다.** 디스크 읽기가 포화돼 다른 쿼리까지 느려졌다.

5. **`phone_last4` 생성 컬럼 + 인덱스.** 결과가 많으면 이름 첫 글자와 함께 입력받는다. 적용 후 p95 8.4초 → 40ms.

## 문제 구조 (추상화 코드)

### 변형 A — 앞 와일드카드 검색
① 문제 코드
```sql
SELECT * FROM customer WHERE phone LIKE '%' || :last4;
```
② 고친 코드
```sql
ALTER TABLE customer ADD COLUMN phone_last4 char(4)
  GENERATED ALWAYS AS (right(phone, 4)) STORED;
CREATE INDEX CONCURRENTLY ix_customer_last4 ON customer(phone_last4);
SELECT * FROM customer WHERE phone_last4 = :last4;
```
무엇이 깨졌나: 검색 조건 모양이 인덱스 구조와 맞지 않았고, 시험 데이터 규모가 이를 가렸다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| last4 생성 컬럼 | 스키마 변경 | 작음 | 4자리 외 검색 불가 | 기본 |
| reverse 컬럼 + 접두 검색 | 스키마 변경 | 작음 | 가변 길이 검색 | 자릿수 다양 |
| trigram 인덱스 | 확장 설치 | 중간 | 인덱스 큼 | 부분 문자열 전반 |

**결론**: 검색 조건은 인덱스가 쓸 수 있는 모양으로 바꾸고, 운영 규모에서 계획을 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
