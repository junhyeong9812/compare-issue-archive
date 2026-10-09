# issue/migrun/add-column-default-rewrite-lock — 정리 (힌트)

태그: `lock` · `deploy`

## 전체 흐름

```
ADD COLUMN ... DEFAULT 0                 → 메타데이터만 변경 (즉시)
ADD COLUMN ... DEFAULT gen_random_uuid() → 행마다 값 계산 → 테이블 재작성
   └ ACCESS EXCLUSIVE 잠금 7분 → 승인 INSERT 대기 → 타임아웃
```

## 핵심 문장

- 상수 기본값은 메타데이터만 바꾸지만, **변동 함수 기본값**은 행마다 값을 계산해 **테이블을 재작성**한다.
- 재작성 동안 `ACCESS EXCLUSIVE` 잠금이라 읽기·쓰기 모두 막힌다.
- `lock_timeout`이 없으면 잠금 대기 자체가 뒤 요청을 줄 세운다.
- 큰 테이블은 기본값 없는 컬럼 추가 → 배치 채우기 → 기본값 설정 순서로 나눈다.
