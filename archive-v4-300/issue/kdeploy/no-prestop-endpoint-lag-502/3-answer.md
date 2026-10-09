# issue/kdeploy/no-prestop-endpoint-lag-502 — 정답

태그: `deploy` · `race`

## 정답
<!-- 질문 1:1 대응 -->

1. **connection refused로 LB가 502를 낸다.**

2. **요청이 앱 프로세스에 도달하지 못했기 때문이다.** LB 접근 로그의 `upstream_status: -`로만 보였다.

3. **생긴다.** 리스너를 닫은 뒤 들어오는 새 요청이 문제다.

4. **`preStop: sleep 10` + `terminationGracePeriodSeconds`를 그보다 크게.** 그 뒤 SIGTERM으로 graceful shutdown.

## 문제 구조 (추상화 코드)

### 변형 A — 종료 훅 없이 즉시 종료
① 문제 코드
```yaml
spec:
  terminationGracePeriodSeconds: 30
  containers: [{ name: settle-api }]
```
② 고친 코드
```yaml
spec:
  terminationGracePeriodSeconds: 45
  containers:
  - name: settle-api
    lifecycle:
      preStop: { exec: { command: ["sleep", "10"] } }
```
무엇이 깨졌나: 종료 신호와 트래픽 차단이 순서대로 일어난다고 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| preStop sleep | 이미지에 sleep 존재 | 배포 10초 증가 | 대기보다 긴 전파 지연 | 기본 |
| LB 재시도 | 멱등 요청만 | 작음 | 비멱등 요청 중복 | 선택하지 않은 방법 |
| 앱이 SIGTERM 후 N초 계속 수신 | 앱 수정 | 작음 | 서비스마다 구현 | preStop 불가 이미지 |

**결론**: preStop 대기를 표준 템플릿에 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
