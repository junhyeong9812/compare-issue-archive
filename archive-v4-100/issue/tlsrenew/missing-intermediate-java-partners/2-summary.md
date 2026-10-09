# issue/tlsrenew/missing-intermediate-java-partners — 정리 (힌트)

태그: `tls` · `deploy`

## 전체 흐름

```
서버 → [leaf] 만 전송
브라우저: AIA 로 중간 인증서 자동 다운로드 → 성공
자바 클라이언트: AIA 미사용 → PKIX path building failed
```

## 핵심 문장

- 서버는 **리프 + 중간 인증서 체인**을 보내야 한다 — 일부 클라이언트만 빠진 체인을 스스로 채운다.
- 브라우저 기반 점검은 이 문제를 못 잡는다.
- `tlsrenew`의 출력 파일 중 `cert.pem`(리프)과 `fullchain.pem`(체인) 중 잘못된 것을 썼다.
- 검증은 `openssl s_client -showcerts`로 체인 개수와, 자바 클라이언트 합성 점검으로 한다.
