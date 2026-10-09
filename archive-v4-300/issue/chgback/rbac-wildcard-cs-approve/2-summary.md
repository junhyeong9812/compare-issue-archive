# issue/chgback/rbac-wildcard-cs-approve — 정리 (힌트)

태그: `security` · `authorization`

## 전체 흐름

```
역할 정의 변경 (조회 권한 묶음 정리)
  CS_AGENT: chargeback:*   ← 원래 chargeback:read
      │  chargeback:accept 포함
      ▼
상담원이 화면의 '수용' 버튼 사용 → 가맹점 대신 회사 부담 처리
```

## 핵심 문장

- 와일드카드는 **나중에 생기는 권한까지** 포함한다 — accept는 정리 이후 추가된 권한이었다.
- 권한 변경 리뷰가 역할 이름만 보고 실제 권한 목록을 펼쳐 보지 않았다.
- 교정: 와일드카드 금지, 권한 변경 시 펼친 목록 diff를 리뷰.
