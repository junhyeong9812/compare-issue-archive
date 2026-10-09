# issue/notifyhub/ios-sandbox-endpoint-prod-build — 정리 (힌트)

태그: `environment-drift` · `deploy`

## 전체 흐름

```
설정 이관: profile=prod 인데 ios.endpoint 키가 누락 → 기본값 api.sandbox.push.apple.com 사용
운영 앱 토큰 → sandbox 엔드포인트 → 400 BadDeviceToken
notifyhub: BadDeviceToken 을 "토큰 무효"로 보고 토큰 삭제   ← 피해 확대
```

## 핵심 문장

- iOS 푸시 토큰은 **발급 환경(개발/운영)에 묶인다** — 운영 토큰을 개발 엔드포인트로 보내면 `BadDeviceToken`이다.
- 설정 이관에서 키 하나가 빠져 코드 기본값(개발용)이 쓰였다.
- 더 나빴던 점: `BadDeviceToken`을 받으면 토큰을 지우는 로직 때문에 6시간 동안 iOS 토큰 41만 개가 삭제됐다.
- 대처: 기본값 제거(키 없으면 기동 실패), 토큰 삭제 비율 급증 시 삭제 정지 회로.
