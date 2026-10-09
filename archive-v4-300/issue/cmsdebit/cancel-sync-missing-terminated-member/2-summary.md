# issue/cmsdebit/cancel-sync-missing-terminated-member — 정리 (힌트)

태그: `data-sync` · `customer-impact`

## 전체 흐름

```
회원 해지 (고객센터 화면) → 사내 member.st = TERMINATED
      │  cmsdebit member del 호출 없음 (모바일 해지 경로에만 있음)
      ▼
출금 신청 대상 = CMS 등록 회원 목록 기준 배치 → 해지 회원 포함
```

## 핵심 문장

- 해지 경로가 **모바일·고객센터 두 개**였고 CMS 해지 호출은 모바일에만 있었다.
- 출금 대상 선정이 사내 상태가 아니라 CMS 등록 목록을 기준으로 했다.
- 교정: 해지를 단일 서비스 메서드로 모으고, 출금 대상은 사내 활성 상태와 교집합.
