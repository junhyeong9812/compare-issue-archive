# issue/sharefs/inode-exhaustion-temp-files — 정리 (힌트)

태그: `capacity` · `resource-limit`

## 전체 흐름

```
df -h: 60% 사용
df -i: 100% 사용 (임시 파일 3,100만 개)
새 파일 생성 → ENOSPC
```

## 핵심 문장

- 파일 시스템은 **용량과 별도로 inode 수**가 한정된다 — 작은 파일이 많으면 inode가 먼저 바닥난다.
- 오류 메시지가 용량 부족과 같아 헷갈린다 — `df -i`로 확인한다.
- 지우면 즉시 회복되지만, 열린 파일 핸들이 남은 파일은 프로세스 종료까지 회수되지 않는다.
- inode 사용률 경보와 임시 파일 정리 주기를 둔다.
