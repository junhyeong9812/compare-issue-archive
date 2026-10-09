# issue/migrun/add-column-default-rewrite-lock — 변동 기본값을 가진 컬럼 추가는 테이블 전체를 다시 쓰며 배타 잠금을 잡는다 — 질문

## 질문
1. (예측) `ALTER TABLE approval ADD COLUMN trace_id uuid DEFAULT gen_random_uuid()`를 1.2억 행 테이블에 실행하면 얼마나 걸리고 그동안 무엇이 막히는가?
2. (왜) 같은 형태의 `DEFAULT 0` 컬럼 추가는 지난달 1초 만에 끝났다. 왜 이번엔 달랐나?
3. (경계) `migrun`의 `lock_timeout` 설정이 있었다면?
4. (연결) 큰 테이블 컬럼 추가는 어떤 순서로 나눠야 하나?

## 복습 기록
| 날짜 | 결과 | 틀린 질문 | 다음 복습 |
|------|------|-----------|-----------|
| | | | |
