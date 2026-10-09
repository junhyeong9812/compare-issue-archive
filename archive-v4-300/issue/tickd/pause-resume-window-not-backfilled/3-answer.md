# issue/tickd/pause-resume-window-not-backfilled — 정답

태그: `silent-failure` · `ops-procedure`

## 정답
<!-- 질문 1:1 대응 -->

1. **다음 정규 회차(03:10) 하나뿐이다.** 정지 중 회차 12개는 버려진다.

2. **재시도 작업이 유일한 처리 경로였고, 입력이 시간 구간 기준이었다.** 214건은 PENDING 상태로 9일 동안 남아 고객 문의로 발견됐다.

3. **DB 점검 중에는 실패만 쌓는다.** 정지 자체는 맞았고, 입력을 상태 기준으로 바꾸는 것이 핵심이다.

4. **누락 건의 발생 시각이 정지 구간과 겹치는지 본다.** 겹치면 이 사건, 흩어져 있으면 재시도 로직 자체를 본다.

## 문제 구조 (추상화 코드)

### 변형 A — 회차 시각 기준 입력
① 문제 코드
```sql
SELECT * FROM refund WHERE status='FAILED'
   AND failed_at >= :fire_time - interval '10 minutes' AND failed_at < :fire_time;
```
② 고친 코드
```sql
SELECT * FROM refund WHERE status='FAILED' AND retry_state='PENDING'
   AND next_retry_at <= now()
 ORDER BY failed_at LIMIT 500;
```
무엇이 깨졌나: "회차는 항상 실행된다"는 가정 위에 입력 구간을 만들었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 상태 기준 입력 | 재시도 상태 컬럼 | 작음 | 없음 | 기본 |
| 재개 후 수동 백필 | 절차서 | 작음 | 절차 누락 | 보조 |

**결론**: 재처리 작업은 시간 구간이 아니라 상태로 대상을 고르고, 재개 절차에 잔여 확인을 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
