# issue/metricdb/rollup-avg-on-counter-monthly-report — 정답

태그: `data-semantics` · `retention`

## 정답
<!-- 질문 1:1 대응 -->

1. **재배포로 카운터가 리셋된 구간마다 증가분이 깎이거나 부풀어 2~4% 차이가 난다.**

2. **이번 달은 원본 보관 범위 안이었다.**

3. **리셋은 여전히 뭉개진다.** 카운터는 `increase` 합을 롤업해야 한다.

4. **원장 DB 일 집계 테이블.** 리포트의 원천을 바꿨다.

## 문제 구조 (추상화 코드)

### 변형 A — 카운터에 평균 롤업
① 문제 코드
```yaml
rollup: { resolution: 5m, aggregate: avg, after: 30d }
```
② 고친 코드
```yaml
rollup:
  - { match: "*_total", resolution: 5m, aggregate: increase_sum, after: 30d }
  - { match: "*",       resolution: 5m, aggregate: avg,          after: 30d }
```
무엇이 깨졌나: 메트릭 유형에 따라 다른 집계가 필요하다는 것을 무시했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 리포트 원천을 원장으로 | 집계 테이블 | 중간 | 없음 | 기본 |
| 카운터 롤업 함수 교정 | 설정 | 작음 | 이미 롤업된 과거는 복구 불가 | 함께 |
| 원본 보관 연장 | 저장 비용 | 중간 | 비용 | 보조 |

**결론**: 업무 리포트는 원장에서 내고 메트릭 롤업은 유형별로 교정한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
