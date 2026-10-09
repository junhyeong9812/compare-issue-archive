# issue/replwatch/replica-apply-paused-by-report-query — 정답

태그: `stale-read` · `config`

## 정답
<!-- 질문 1:1 대응 -->

1. **리포트 쿼리가 끝난 뒤에야 보인다.** 복제본이 WAL을 받긴 하지만 적용을 미뤄 계좌 행이 옛 값이다.

2. **6월부터 월간 리포트가 어드민 복제본에서 돌기 시작했기 때문이다.** 지연 설정이 `-1`이라 리포트가 도는 동안 적용이 계속 밀렸다.

3. **생긴다.** 적용은 WAL 순서대로라, 충돌한 한 레코드에서 멈추면 그 뒤 모든 변경이 기다린다.

4. **`replay_lag`와 `write_lag`를 나눠 본다.** 수신은 됐는데 적용만 늦으면 복제본 충돌이다.\
캐시 TTL이면 복제본에서 직접 조회했을 때 새 값이 보인다.

5. **주 DB에서 읽는다.** 변경 직후 같은 사용자의 조회(read-your-writes)는 몇 초간 주 DB로 보낸다.

## 문제 구조 (추상화 코드)

### 변형 A — 긴 분석 쿼리와 온라인 조회가 같은 복제본을 씀
① 문제 코드
```yaml
replica-admin:
  max_standby_streaming_delay: -1
  consumers: [admin-api, monthly-report]
```
② 고친 코드
```yaml
replica-admin:
  max_standby_streaming_delay: 30s
  consumers: [admin-api]
replica-report:
  max_standby_streaming_delay: -1
  consumers: [monthly-report]
```
무엇이 깨졌나: 온라인 조회의 신선도와 리포트 완주라는 상충하는 요구를 한 복제본 설정으로 묶었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 리포트 전용 복제본 | 서버 1대 추가 | 비용 | 전용 복제본 지연은 남음 | 기본 |
| 지연 상한 30초 | 리포트 취소 허용 | 작음 | 리포트가 반복 취소 | 온라인 복제본 |
| 쓰기 직후 주 DB 조회 | 라우팅 로직 | 작음 | 주 DB 부하 | 어드민 변경 화면 |

**결론**: 리포트를 전용 복제본으로 옮기고 온라인 복제본의 지연 상한을 둔다. `replay_lag` 경보를 1분으로 건다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
