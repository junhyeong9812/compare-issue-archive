# issue/remitx/sha-charge-intermediary-deduct — 정리 (힌트)

태그: `contract-drift` · `fees`

## 전체 흐름

```
remitx send --amount 2,000 USD --charge (생략 → SHA)
      │  SHA: 송금 은행 수수료는 송금인, 중계·수취 은행 수수료는 수취인
      ▼
중계은행 25 USD 차감 → 셀러 수취 1,975 USD
사내 정산서: 2,000 USD 지급 완료
```

## 핵심 문장

- SWIFT 수수료 부담은 **OUR(송금인 전부)·SHA(나눔)·BEN(수취인 전부)** 세 가지다.
- `remitx`는 `--charge`를 생략하면 SHA다 — 셀러 계약은 "정산액 전액 지급"이었다.
- 중계은행을 거치는 통화·국가 조합만 차감돼 일부 셀러만 문의했다.
- 교정: OUR 명시, 차감 발생 건은 차액 추가 송금.
