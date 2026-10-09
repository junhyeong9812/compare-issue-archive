# issue/lbctl/health-check-path-behind-auth — 정리 (힌트)

태그: `deploy` · `outage`

## 전체 흐름

```
인증 필터 v2 → /internal/health 도 401
LB 헬스체크: 200 아님 → unhealthy
카나리 1대: 1대만 빠짐 → 나머지가 처리 → 지표 정상
전체 롤아웃: 전부 unhealthy → 503 (fail-closed)
```

## 핵심 문장

- 헬스체크는 **인증 없는 고정 경로**여야 한다 — 필터 범위 변경이 이를 깨뜨렸다.
- 카나리 한 대가 빠져도 나머지가 처리해 지표에 안 보였다 — 카나리의 LB 상태를 봤어야 한다.
- 이 LB는 전부 비정상이면 503을 낸다(fail-closed). 일부 LB는 전부에게 보내는 fail-open 옵션이 있다.
- 필터 제외 목록에 헬스 경로를 명시하고, 배포 검증에 LB 대상 상태를 넣는다.
