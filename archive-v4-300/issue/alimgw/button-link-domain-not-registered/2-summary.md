# issue/alimgw/button-link-domain-not-registered — 정리 (힌트)

태그: `contract-drift` · `deploy`

## 전체 흐름

```
배포: 영수증 링크 receipt.hanbitpay.co.kr → r.hbpay.kr (짧은 도메인)
alimgw send (button.url_mobile = https://r.hbpay.kr/...)
   → 400 {"code":"B220","msg":"button url domain not allowed"}
채널 등록 도메인: hanbitpay.co.kr 만
```

## 핵심 문장

- 알림톡 버튼 URL은 **채널에 등록된 도메인**만 허용된다 — 템플릿 승인 때의 도메인과도 맞아야 한다.
- 새 도메인 등록은 채널 관리자 승인 절차가 있어 배포보다 늦었다.
- 대처: 즉시 롤백 후 도메인 등록·템플릿 재승인을 마치고 다시 배포했다.
