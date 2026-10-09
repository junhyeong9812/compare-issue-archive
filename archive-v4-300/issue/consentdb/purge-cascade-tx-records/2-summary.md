# issue/consentdb/purge-cascade-tx-records — 정리 (힌트)

태그: `irreversible` · `data-retention`

## 전체 흐름

```
member ──(FK ON DELETE CASCADE)──▶ tx_record
파기 배치: DELETE FROM member WHERE withdrawn_at < now() - 30d
  → tx_record 연쇄 삭제   ← 5년 보관 대상

2023-06 스키마 정리 때 FK 를 RESTRICT → CASCADE 로 바꿈 (고아 행 정리 목적)
교정: 파기 = 개인 식별 컬럼 비식별화(UPDATE), 행은 보관 기간 후 삭제
```

## 핵심 문장

- 외래 키 `ON DELETE CASCADE` 때문에 **회원 삭제가 거래 기록 삭제**로 번졌다.
- 전자금융거래 기록은 **5년 보관** 대상이다 — 개인정보 파기 대상과 다르다.
- 2023-06 스키마 정리에서 FK가 RESTRICT → CASCADE로 바뀐 것이 계기였다.
- 파기는 **비식별화(UPDATE)**로 하고, 행 삭제는 보관 기간 정책으로 따로 한다.
