# issue/alertrt/inhibit-by-stale-cluster-alert — 정답

태그: `observability` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **억제돼 호출되지 않는다.**

2. **ClusterDown은 억제 소스라 자신도 라우팅에서 '정보'로만 갔다.**

3. **`equal: [env, cluster]`.**

4. **억제된 critical 경보가 5분 이상이면 별도 경보, 24시간 넘은 소스 경보 주간 점검.**

## 문제 구조 (추상화 코드)

### 변형 A — 억제 범위가 env 전체
① 문제 코드
```yaml
inhibit_rules:
  - source_matchers: [alertname="ClusterDown"]
    target_matchers: [severity=~".*"]
    equal: [env]
```
② 고친 코드
```yaml
inhibit_rules:
  - source_matchers: [alertname="ClusterDown"]
    target_matchers: [severity!="page-always"]
    equal: [env, cluster]
```
무엇이 깨졌나: 소음 줄이기 규칙이 영향 범위를 너무 넓게 잡았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| equal에 cluster 추가 | 라벨 일관성 | 작음 | 라벨 누락 경보 | 기본 |
| 억제 메타 경보 | 메트릭 | 작음 | 없음 | 함께 |
| 억제 규칙 제거 | 없음 | 작음 | 장애 시 경보 폭주 | 선택하지 않은 방법 |

**결론**: 억제 범위를 좁히고 억제 자체를 감시한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
