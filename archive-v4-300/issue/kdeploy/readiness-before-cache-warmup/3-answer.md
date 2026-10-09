# issue/kdeploy/readiness-before-cache-warmup — 정답

태그: `deploy` · `performance`

## 정답
<!-- 질문 1:1 대응 -->

1. **기동 5초 뒤부터 받는다.** 캐시는 70초 뒤에야 찬다.

2. **새 파드가 Ready가 되자마자 옛 파드가 종료됐기 때문이다.** 2분 안에 대부분의 트래픽이 차가운 파드로 갔다.

3. **당장은 되지만 깨지기 쉽다.** 가맹점이 늘어 적재가 길어지면 다시 난다.

4. **캐시 적재 완료를 확인한다.** `/ready`가 워밍업 플래그를 보고, `/live`는 프로세스 생존만 본다.

## 문제 구조 (추상화 코드)

### 변형 A — readiness가 웹 서버 기동만 확인
① 문제 코드
```yaml
readinessProbe:
  httpGet: { path: /health, port: 8080 }
```
② 고친 코드
```yaml
startupProbe:
  httpGet: { path: /ready, port: 8080 }
  failureThreshold: 30
  periodSeconds: 5
readinessProbe:
  httpGet: { path: /ready, port: 8080 }   # warmup 완료 시 200
```
무엇이 깨졌나: 'Ready'의 정의가 서비스 가능 상태가 아니라 프로세스 기동이었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| /ready 에 워밍업 플래그 | 앱 수정 | 작음 | 워밍업 실패 시 영원히 NotReady | 기본 |
| initialDelaySeconds 증가 | 없음 | 작음 | 적재 시간 변화에 취약 | 임시 |
| maxSurge 증가·maxUnavailable 0 | 여유 자원 | 중간 | 배포 시간 증가 | 함께 |

**결론**: readiness를 워밍업 완료로 정의하고 maxUnavailable을 0으로 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
