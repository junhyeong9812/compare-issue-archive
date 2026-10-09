# issue/tracepipe/head-sampling-drops-error-traces — 정답

태그: `observability` · `sampling`

## 정답
<!-- 질문 1:1 대응 -->

1. **약 10건이다.**

2. **샘플링은 오류율과 무관하게 1%였다.**

3. **그 서비스 스팬만 남고 트레이스가 끊긴다.**

4. **수집기 테일 샘플링: 오류·1초 초과 100%, 나머지 1%.** 트레이스 ID 기준 로드밸런싱 계층을 앞에 둔다.

## 문제 구조 (추상화 코드)

### 변형 A — 헤드 샘플링만 사용
① 문제 코드
```yaml
sampler: { type: probabilistic, rate: 0.01 }   # 게이트웨이
```
② 고친 코드
```yaml
# 게이트웨이: 100% 전송, 수집기에서 결정
processors:
  tail_sampling:
    decision_wait: 10s
    policies:
      - { name: errors, type: status_code, status_codes: [ERROR] }
      - { name: slow, type: latency, threshold_ms: 1000 }
      - { name: rest, type: probabilistic, sampling_percentage: 1 }
```
무엇이 깨졌나: 비용을 줄이는 결정을 정보가 가장 적은 시점에 내렸다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 테일 샘플링 | 수집기 메모리·라우팅 | 중간 | 수집기 과부하 시 손실 | 기본 |
| 헤드 비율 상향 | 저장 비용 | 큼 | 비용 폭증 | 선택하지 않은 방법 |
| 오류 시 로그에 트레이스 ID | 로그 | 작음 | 스팬 없음 | 보조 |

**결론**: 결정을 수집기로 옮겨 오류·지연 트레이스를 모두 남긴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
