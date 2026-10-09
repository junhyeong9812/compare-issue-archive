# issue/tickd/leader-lease-gc-pause-double-run — 정답

태그: `concurrency` · `split-brain`

## 정답
<!-- 질문 1:1 대응 -->

1. **1.9에는 없다.** A의 메모리상 리더 플래그는 true로 남아 있다.

2. **Full GC가 리스 TTL(10초)을 넘은 적이 없었다.** 그날 A는 리포트 캐시 적재로 힙이 거의 찼다.

3. **확률은 줄지만 같은 구조다.** 펜싱 토큰과 멱등 키가 있어야 정지 길이와 무관하게 막힌다.

## 문제 구조 (추상화 코드)

### 변형 A — 리더 플래그만 보고 실행
① 문제 코드
```java
if (leader.isLeader()) job.run(round);
```
② 고친 코드
```java
long gen = leader.currentGeneration();
if (runLog.claim(job.id(), round, gen)) job.run(round);   // UPDATE ... WHERE gen >= stored_gen
// 발송 측: (job, round, member) 유니크 키로 중복 차단
```
무엇이 깨졌나: 리더 여부를 시간이 지난 로컬 상태로 판정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 펜싱 토큰 + 조건부 claim | 공유 저장소 | 작음 | 없음 | 기본 |
| 회차 키 멱등 | 유니크 제약 | 작음 | 키 설계 실수 | 함께 적용 |
| TTL 증가 | 없음 | 장애 전환 지연 | 재발 | 선택하지 않음 |

**결론**: 리더 선출만 믿지 말고 실행 기록에 세대 번호를 걸어 막고, 발송은 멱등 키로 지킨다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
