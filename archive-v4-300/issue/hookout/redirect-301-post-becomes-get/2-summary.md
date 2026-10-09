# issue/hookout/redirect-301-post-becomes-get — 정리 (힌트)

태그: `silent-failure` · `http-semantics`

## 전체 흐름

```
가맹점 등록 URL: http://shop-a.example/hook
가맹점이 https 로 이전 → 301 Location: https://shop-a.example/hook
hookout HTTP 클라이언트: 리다이렉트 자동 추적 ON
   301 수신 → 표준 관행대로 메서드를 GET 으로 바꾸고 본문 버림
   GET https://shop-a.example/hook → 가맹점 프레임워크 기본 페이지 200
hookout: 최종 응답 200 → DELIVERED
```

## 핵심 문장

- HTTP 클라이언트는 **301·302를 따라갈 때 POST를 GET으로 바꾼다**(역사적 관행) — 본문이 사라진다. 307·308만 메서드를 유지한다.
- 가맹점의 GET 엔드포인트가 200을 돌려줘 실패가 "성공"으로 기록됐다.
- http로 등록한 채 https로 이전한 가맹점 23곳이 해당됐다.
- 대처: 웹훅 발송은 리다이렉트를 따라가지 않고 3xx를 실패로 기록, 가맹점에 URL 갱신 요청 알림.
