# issue/blobgw/presigned-url-temp-credential-expiry — 정리 (힌트)

태그: `auth` · `config`

## 전체 흐름

```
서명: 임시 키 (만료 1h) + expires 7d
검증: URL 만료 7d 이내 OK / 서명 키 만료 → ExpiredToken
2월: 장기 키(사용자 키)로 서명 → 7d 유효
3월: 보안 점검으로 파드 역할 전환 → 1h 만에 만료
```

## 핵심 문장

- 서명 URL의 실제 수명은 **URL 만료와 서명 자격 증명 만료 중 짧은 쪽**이다.
- 역할 전환은 보안 개선이었지만, 서명 수명이 바뀐다는 점을 몰랐다.
- 긴 링크가 필요하면 짧은 서명 URL을 **요청 시마다 새로 발급**하는 리다이렉트 엔드포인트가 낫다.
- 만료 오류는 `ExpiredToken`(자격 증명)과 `Request has expired`(URL)로 구분된다.
