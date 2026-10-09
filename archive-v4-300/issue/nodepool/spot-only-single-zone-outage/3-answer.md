# issue/nodepool/spot-only-single-zone-outage — 정답

태그: `availability` · `capacity`

## 정답
<!-- 질문 1:1 대응 -->

1. **6개가 동시에 종료되고 대체 노드가 뜰 때까지 12분간 불가.**

2. **분산 제약이 없었고 spot-a가 가장 쌌다.**

3. **옮길 자리가 없었다.** 온디맨드 여유가 0이었다.

4. **영역 분산 제약 + 레플리카 3분의 1 온디맨드 고정.**

## 문제 구조 (추상화 코드)

### 변형 A — 분산 제약 없는 배포
① 문제 코드
```yaml
spec:
  replicas: 6
  template: { spec: { nodeSelector: { pool: spot } } }
```
② 고친 코드
```yaml
spec:
  replicas: 6
  template:
    spec:
      topologySpreadConstraints:
        - { maxSkew: 1, topologyKey: topology.kubernetes.io/zone, whenUnsatisfiable: DoNotSchedule }
      affinity: { nodeAffinity: { preferredDuringSchedulingIgnoredDuringExecution: [...] } }  # on-demand 2개 이상
```
무엇이 깨졌나: 비용 최적화가 가용성 제약 없이 돌았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 영역 분산 + 온디맨드 일부 | 비용 소폭 증가 | 중간 | 온디맨드 부족 | 기본 |
| PDB | 설정 | 작음 | 회수엔 무력 | 함께 |
| 전부 온디맨드 | 비용 | 큼 | 없음 | 선택하지 않은 방법 |

**결론**: 사용자 대면 서비스는 영역 분산과 온디맨드 하한을 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
