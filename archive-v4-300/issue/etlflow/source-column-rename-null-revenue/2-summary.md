# issue/etlflow/source-column-rename-null-revenue — 정리 (힌트)

태그: `schema-drift` · `silent-failure` · `data-quality`

## 전체 흐름

```
8/05 주문 DB 배포: app_discount_amt → app_discount_amount (앱 주문 전용 컬럼)
etlflow 매핑: 원천에 없는 컬럼 → NULL 로 채움 (경고 로그만)
앱 매출 = 결제액 − 앱 할인액 → 할인액 NULL → 매출 NULL (NULL 전파) → 집계 0
다른 채널: 해당 컬럼 미사용 → 정상
```

## 핵심 문장

- 원인은 **원천 컬럼 이름 변경**을 ETL이 오류가 아닌 **NULL 채움**으로 처리한 것이다.
- 그 컬럼은 **앱 주문 계산식**에만 쓰여 앱 채널만 0이 됐다.
- 주문 **건수는 정상**이라 건수 기반 품질 검사를 통과했다.
- 대처는 원천 스키마 변경 시 ETL 실패(엄격 모드), 금액 필드 NULL 비율 검사, 원천 배포에 데이터팀 리뷰.
