# issue/auditsink/shutdown-buffer-no-flush — 정리 (힌트)

태그: `silent-failure` · `lifecycle`

## 전체 흐름

```
앱 이벤트 ──▶ 클라이언트 버퍼 ──(5초마다 flush)──▶ auditsink

배포: SIGTERM → 앱 main 종료 → JVM 종료 (shutdown hook 없음)
      버퍼에 남은 0~5초치 이벤트 → 사라짐

차이 ≈ 배포 횟수 × 파드 수 × 평균 2.5초 분량
노드 장애(SIGKILL) → hook 이 있어도 못 지킴 → 디스크 스풀 필요
```

## 핵심 문장

- 감사 클라이언트 2.x는 **종료 hook에서 flush하지 않는다** — SIGTERM 시 버퍼가 버려진다.
- 앱이 SIGTERM에 바로 종료해 쿠버네티스 유예 30초를 쓰지 않았다.
- 차이는 **배포 횟수 × 파드 수**에 비례한다 — 일반 장애로 보기 어려운 패턴이다.
- 종료 hook flush를 넣고, 감사처럼 유실이 안 되는 이벤트는 로컬 디스크 스풀을 쓴다.
