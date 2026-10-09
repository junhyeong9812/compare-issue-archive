# issue/flowctl/nondeterministic-replay-after-deploy — 정답

태그: `deploy` · `determinism`

## 정답
<!-- 질문 1:1 대응 -->

1. **비결정성 오류로 워크플로 태스크가 실패하고 끝없이 재시도된다.** 실행은 RUNNING으로 남는다.

2. **이력이 새 코드로 처음부터 만들어졌다.** 배포 시점에 진행 중이던 412건만 멈췄다.

3. **이미 그 지점을 지난 실행에는 안전하지 않을 수 있다.** 원칙적으로 명령 순서를 바꾸는 변경은 모두 버전 분기를 쓴다.

4. **멈춘 실행의 워크플로 태스크 실패 사유를 본다.** NonDeterministic이면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 버전 분기 없이 단계 삽입
① 문제 코드
```java
credit = activities.creditCheck(app);
fraud  = activities.fraudScore(app);          // 새로 끼워 넣음
income = activities.incomeCheck(app);
```
② 고친 코드
```java
credit = activities.creditCheck(app);
int v = Workflow.getVersion("fraud-score", Workflow.DEFAULT_VERSION, 1);
if (v >= 1) fraud = activities.fraudScore(app);
income = activities.incomeCheck(app);
// CI: 운영 이력 샘플 100건 재생 테스트
```
무엇이 깨졌나: 워크플로 코드를 일반 코드처럼 바꿨다 — 진행 중 실행의 이력과 호환돼야 한다는 계약을 놓쳤다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 버전 분기 | 엔진 지원 | 작음 | 분기 정리 부담 | 기본 |
| 진행 중 실행 종료 후 배포 | 실행 짧음 | 큼(대기) | 장기 실행엔 불가 | 짧은 워크플로 |
| 재생 테스트 CI | 이력 샘플 | 중간 | 샘플 편향 | 함께 적용 |

**결론**: 워크플로 변경은 버전 분기로 하고, 운영 이력 재생 테스트를 배포 조건으로 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
