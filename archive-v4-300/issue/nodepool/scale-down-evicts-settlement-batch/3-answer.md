# issue/nodepool/scale-down-evicts-settlement-batch — 정답

태그: `duplicate` · `batch`

## 정답
<!-- 질문 1:1 대응 -->

1. **앞 50분치 줄이 두 번 들어간다.**

2. **배치 파드에 `nodepool.io/safe-to-evict: "false"`가 없었다.**

3. **거르지 않는다.** 중복 38건이 실제 지급됐다.

4. **출력은 임시 파일에 쓰고 완료 시 rename, 파일 생성 전 지급 ID 유니크 검사.**

## 문제 구조 (추상화 코드)

### 변형 A — 최종 파일에 직접 append
① 문제 코드
```java
try (var w = Files.newBufferedWriter(out, APPEND, CREATE)) {
    for (Payout p : payouts) w.write(line(p));
}
```
② 고친 코드
```java
Path tmp = out.resolveSibling(out.getFileName() + ".tmp");
try (var w = Files.newBufferedWriter(tmp, TRUNCATE_EXISTING, CREATE)) {
    for (Payout p : payouts) w.write(line(p));
}
Files.move(tmp, out, ATOMIC_MOVE);
```
무엇이 깨졌나: 배치가 중간에 죽지 않는다고 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 임시 파일 + 원자 교체 | 코드 수정 | 작음 | 없음 | 기본 |
| 축출 금지 주석 | 파드 주석 | 작음 | 노드 장애엔 무력 | 함께 |
| 지급 ID 중복 검사 | 검사 단계 | 작음 | 검사 누락 | 안전망 |

**결론**: 재시작이 안전한 출력 구조를 기본으로 하고 축출 금지는 보조로 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
