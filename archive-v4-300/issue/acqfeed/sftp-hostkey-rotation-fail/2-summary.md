# issue/acqfeed/sftp-hostkey-rotation-fail — 정리 (힌트)

태그: `environment-drift` · `fail-closed`

## 전체 흐름

```
KB카드 SFTP 서버 교체 (IP 동일, 호스트 키 변경)
      │
acqfeed pull --card KB
      │  StrictHostKeyChecking=yes, known_hosts 고정
      ▼
"Host key verification failed" → exit 3 → 재시도 3회 → 실패 경보
```

## 핵심 문장

- 호스트 키 검증 실패는 **정상적인 방어 동작**이다 — 끄면 중간자 공격을 받는다.
- 카드사 공지는 "서버 이전, IP 변경 없음"이었고 새 키 지문은 별도 메일로 왔는데 담당자 부재로 반영이 늦었다.
- 새 키는 공지 지문과 대조한 뒤 `acqfeed hostkey add --card KB --fp <지문>`으로만 넣는다.
