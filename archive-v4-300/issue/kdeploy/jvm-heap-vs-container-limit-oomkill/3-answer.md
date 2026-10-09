# issue/kdeploy/jvm-heap-vs-container-limit-oomkill — 정답

태그: `resource-limit` · `config`

## 정답
<!-- 질문 1:1 대응 -->

1. **한도에 닿으면 커널이 프로세스를 죽인다.** 압축 단계에서 다이렉트 버퍼가 늘 때 넘었다.

2. **경보는 힙 기준이었다.** 컨테이너 메모리 경보가 없었다.

3. **당장은 되지만 힙 비율이 그대로면 힙도 같이 커져 여유가 늘지 않는다.**

4. **컨테이너 `working_set`과 NMT(Native Memory Tracking) 요약.**

## 문제 구조 (추상화 코드)

### 변형 A — 힙 비율을 한도 대비 높게
① 문제 코드
```yaml
resources: { limits: { memory: 2Gi } }
env: [{ name: JAVA_OPTS, value: "-XX:MaxRAMPercentage=85" }]
```
② 고친 코드
```yaml
resources: { limits: { memory: 2Gi } }
env: [{ name: JAVA_OPTS, value: "-XX:MaxRAMPercentage=65 -XX:MaxDirectMemorySize=300m" }]
```
무엇이 깨졌나: 한도를 힙 한도로 착각했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 힙 비율 하향 + 다이렉트 상한 | 설정 | 작음 | 힙 부족 시 GC 증가 | 기본 |
| 컨테이너 메모리 경보 | 메트릭 | 작음 | 없음 | 함께 |
| 한도 상향 | 노드 여유 | 비용 | 같은 비율이면 재발 | 보조 |

**결론**: 비힙 여유를 명시적으로 두고, 경보를 컨테이너 기준으로 건다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
