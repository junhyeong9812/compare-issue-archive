# issue/kycflow/webhook-301-post-to-get — 정리 (힌트)

태그: `silent-failure` · `environment-drift` · `protocol`

## 전체 흐름

```
kycflow ──POST http://hook…/kyc/done {result}──▶ 인그레스
                                                   │ 301 Location: https://…
kycflow HTTP 클라이언트: 301 을 따라가며 메서드를 GET 으로 바꾸고 본문 버림 (관행)
        ──GET https://hook…/kyc/done──▶ 핸들러
                                          GET = 헬스체크용 200 "ok"   ← 성공처럼 보임
kycflow: 최종 응답 200 → 전달 성공, 재시도 없음
고객 상태: 결과를 못 받아 REVIEWING 고정

교정: 웹훅 주소를 https 로 재등록 · GET 은 405 · 리다이렉트 응답 로그 감시
```

## 핵심 문장

- HTTP 클라이언트 대부분은 **301/302를 따라갈 때 POST를 GET으로 바꾸고 본문을 버린다** — kycflow도 그렇다.
- 수신 경로의 GET이 헬스체크용 **200**을 돌려줘서 벤더는 성공으로 기록했다.
- 307/308은 메서드와 본문을 유지한다 — 하지만 등록 주소 자체를 https로 바꾸는 게 맞다.
- '진행 중' 고착은 먼저 **수신 로그의 메서드·상태 코드**를 본다.
