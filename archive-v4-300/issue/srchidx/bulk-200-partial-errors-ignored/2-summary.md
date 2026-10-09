# issue/srchidx/bulk-200-partial-errors-ignored — 정리 (힌트)

태그: `silent-failure` · `partial-failure`

## 전체 흐름

```
bulk 1000건 → HTTP 200
  { "errors": true, "items": [ ..., { "index": { "status": 400,
      "error": { "type": "mapper_parsing_exception" } } }, ... ] }   ← 7건
색인기: if status == 200 → batch.ok   ← items 안 봄
```

## 핵심 문장

- bulk API는 **요청 단위 성공(200)과 문서 단위 결과**가 따로다 — `errors: true`와 `items[].status`를 봐야 한다.
- 빠진 문서는 해외 가맹점 거래처럼 **금액을 천 단위 구분 문자열로 보낸** 경로뿐이었다.
- 다시 보내도 같은 이유로 거부된다 — 원천 데이터 변환을 고쳐야 한다.
- 지연이면 시간이 지나 나타나고, 거부면 영영 안 나타난다 — 원장 대비 문서 수 대사로 가른다.
