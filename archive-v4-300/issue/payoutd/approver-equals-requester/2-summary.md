# issue/payoutd/approver-equals-requester — 정리 (힌트)

태그: `security` · `authorization`

## 전체 흐름

```
수동 지급 요청 (운영자 A) → 승인 대기
승인 API  POST /manual/{id}/approve   (권한: PAYOUT_APPROVER)
      │  A 도 PAYOUT_APPROVER 역할 보유 → 자기 요청 승인 가능
      ▼
2인 결재 규정이 화면에서만 지켜짐 (화면은 자기 건 승인 버튼 숨김)
```

## 핵심 문장

- 2인 결재가 **화면 버튼 숨김**으로만 구현됐다 — API는 막지 않았다.
- 14건 모두 운영자가 스크립트로 API를 직접 호출한 건이었다(긴급 처리 명목).
- 교정: 서버에서 승인자 ≠ 요청자 검사, 감사 로그에 둘 다 기록.
