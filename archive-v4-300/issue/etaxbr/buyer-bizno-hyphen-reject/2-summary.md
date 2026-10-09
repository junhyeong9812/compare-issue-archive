# issue/etaxbr/buyer-bizno-hyphen-reject — 정리 (힌트)

태그: `silent-failure` · `async-status`

## 전체 흐름

```
수수료 정산 → etaxbr issue  (buyer_bizno "123-45-67890")
      │  응답 status ISSUED, exit 0      ← 중계사 저장·이메일 발송까지
      ▼
익일 02:00 중계사 → 국세청 전송
      │  사업자번호 형식 오류 (하이픈 포함 12자) → NTS_REJECTED
      ▼
반려 상태는 etaxbr status <id> 의 nts 필드에만, 콜백 없음
```

## 핵심 문장

- `etaxbr issue`의 `ISSUED`는 **중계사 접수·이메일 발송**까지다 — 국세청 전송 결과는 다음 날 `nts` 필드에 따로 생긴다.
- 중계사는 사업자번호의 **하이픈을 접수 단계에서 검사하지 않고**, 국세청 전송 단계에서 반려된다.
- 사내 거래처 마스터 중 2024-01 이관분만 하이픈이 붙어 있었다 — 그래서 일부 거래처만 걸렸다.
- 완료 판정은 `nts=SENT`로 하고, 반려는 수정 후 같은 문서번호로 재전송한다.
