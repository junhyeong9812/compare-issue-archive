# issue/cdcpipe/toast-unchanged-placeholder-overwrite — 정리 (힌트)

태그: `data-semantics` · `silent-failure`

## 전체 흐름

```
UPDATE product SET price=... WHERE id=...   (description 은 2KB 넘어 TOAST 저장, 미변경)
REPLICA IDENTITY DEFAULT → WAL 에 미변경 TOAST 값 미포함
cdcpipe 이벤트: description = "__debezium_unavailable_value"
인덱서: 이벤트 값 그대로 문서 전체 갱신 → 설명이 자리표시로 덮임
```

## 핵심 문장

- PostgreSQL은 바뀌지 않은 TOAST 컬럼 값을 WAL에 싣지 않는다 — 커넥터는 **자리표시 문자열**을 넣는다.
- 설명이 2KB 이하인 상품은 TOAST가 아니어서 정상이었다 — "일부 상품만" 깨졌다.
- 인덱서가 이벤트를 문서 전체 교체로 처리해 자리표시가 실제 값이 됐다.
- 대처: 인덱서가 자리표시 필드를 갱신 대상에서 빼는 부분 갱신으로 변경. 테이블 `REPLICA IDENTITY FULL`은 WAL 증가로 보류.
