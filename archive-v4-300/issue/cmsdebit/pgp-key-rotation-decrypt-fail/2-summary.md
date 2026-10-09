# issue/cmsdebit/pgp-key-rotation-decrypt-fail — 정리 (힌트)

태그: `credential-rotation` · `fail-closed`

## 전체 흐름

```
대행사 PGP 키 교체 공지 (보안 담당자 메일로만 수신)
결과 파일 → 새 공개키로 암호화
cmsdebit result fetch → 복호화 실패 (no secret key) → exit 2, 경보
```

## 핵심 문장

- 실패는 즉시 보였다 — 문제는 키 교체 공지가 운영팀에 전달되지 않은 것이다.
- 새 키 쌍은 사내가 생성해 공개키를 대행사에 등록하는 방식이었는데, 대행사가 일방적으로 바꾼 것이 아니라 사내 키 만료(2년)로 대행사가 갱신을 요청한 것이었다.
- 교정: 키 만료일 관리, 공지 수신 주소를 팀 공용으로.
