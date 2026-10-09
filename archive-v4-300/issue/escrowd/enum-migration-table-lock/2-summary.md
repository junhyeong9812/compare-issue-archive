# issue/escrowd/enum-migration-table-lock — 정리 (힌트)

태그: `deploy` · `database`

## 전체 흐름

```
마이그레이션: ALTER TABLE escrow_order MODIFY status ENUM(..., 'DISPUTED')
테이블 4,200만 행 → 테이블 복사 방식 변경 → 쓰기 잠금
      │  결제 완료 처리(escrow_order INSERT) 대기
      ▼
10분 후 완료, 대기 요청 일부 타임아웃
```

## 핵심 문장

- ENUM 끝에 값을 추가해도 엔진·버전에 따라 **테이블 복사**가 될 수 있다.
- 스테이징 테이블은 3만 행이라 1초에 끝났다.
- 교정: 대형 테이블은 온라인 스키마 변경 도구 사용, 상태값은 ENUM 대신 코드 테이블.
