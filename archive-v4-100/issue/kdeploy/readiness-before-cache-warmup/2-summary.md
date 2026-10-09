# issue/kdeploy/readiness-before-cache-warmup — 정리 (힌트)

태그: `deploy` · `performance`

## 전체 흐름

```
파드 기동 → 웹 서버 열림(5초) → /health 200 → Ready → 트래픽 유입
          └ 수수료 표 적재 중(70초) → 요청마다 DB 직접 조회 → 타임아웃 → 5xx
maxUnavailable 25% → 옛 파드 빠르게 종료
```

## 핵심 문장

- readiness가 **프로세스 생존**만 보면, 의존 데이터가 준비되기 전에 트래픽이 들어온다.
- 롤링 배포는 새 파드가 Ready가 되는 즉시 옛 파드를 줄인다 — 차가운 파드 비율이 빠르게 오른다.
- 고정 지연(`initialDelaySeconds`)은 적재 시간이 바뀌면 다시 깨진다.
- readiness는 **워밍업 완료 플래그**를 확인하게 하고, 활성 상태(liveness)와 분리한다.
