# issue/bkvault/one-file-system-skips-new-volume — 정리 (힌트)

태그: `silent-failure` · `backup`

## 전체 흐름

```
/data/settle        (디스크 A)  → 백업
/data/settle/attach (디스크 B, 9월 마운트) → one_file_system → 빈 디렉터리로 기록
마운트 전 attach 아래 파일(디스크 A) → 마운트로 가려짐, 백업엔 9월 이전 스냅숏만 남음
```

## 핵심 문장

- `one_file_system`은 **다른 파일 시스템에 속한 하위 디렉터리**를 빈 디렉터리로 기록한다 — 경고는 디버그 로그에만 있다.
- 새 디스크 증설 때 백업 설정을 같이 보지 않았다.
- 백업 크기는 다른 데이터 증가에 묻혀 줄지 않았다.
- 분기 복구 연습에 **파일 수·경로 샘플 대조**를 넣는다.
