# issue/tickd/overlap-forbid-silent-skip — 정답

태그: `silent-failure` · `scheduling`

## 정답
<!-- 질문 1:1 대응 -->

1. **SKIPPED로 끝나고 아무 일도 하지 않는다.** 12시 회차는 11~12시 구간만 집계한다 — 10~11시 구간이 비게 된다.

2. **대시보드 빈칸은 "트래픽이 적은 시간"으로 넘겨졌다.** 다음 날 일 정산 합계와 시간별 합계가 1억 2천만 원 차이 나서 추적했다.

3. **누락은 없어지지만 지연이 누적될 수 있다.** 구간을 "마지막 성공 이후"로 정하는 방식이 겹침 정책과 무관하게 안전하다.

4. **비어 있는 시간대 회차의 실행 이력 상태를 본다.** SKIPPED이면 이 사건, 실행됐는데 0건이면 원천 데이터 쪽이다.

## 문제 구조 (추상화 코드)

### 변형 A — 회차 시각으로 고정 구간을 정함
① 문제 코드
```java
void run(Instant fireTime) {
    aggregate(fireTime.minus(1, HOURS), fireTime);
}
```
② 고친 코드
```java
void run(Instant fireTime) {
    Instant from = progressRepo.lastSuccessEnd("hourly-sales");   // 건너뛴 구간도 다음 회차가 처리
    aggregate(from, fireTime);
    progressRepo.save("hourly-sales", fireTime);
}
```
무엇이 깨졌나: 회차마다 고정 구간을 처리하는 설계가 "회차가 실행되지 않을 수 있다"는 스케줄러 계약과 맞지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 마지막 성공 이후 구간 처리 | 진행 위치 저장 | 작음 | 긴 구간 한 번에 처리 | 기본 |
| SKIPPED 경보 | 이력 이벤트 구독 | 작음 | 소음 | 함께 적용 |
| QUEUE 정책 | 없음 | 작음 | 지연 누적 | 짧은 작업 |

**결론**: 주기 작업은 처리 구간을 진행 위치로 정하고, 건너뜀은 경보로 보이게 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
