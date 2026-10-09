# issue/evstream/compacted-tombstone-expired-before-consume — 정답

태그: `silent-failure` · `compaction`

## 정답
<!-- 질문 1:1 대응 -->

1. **받지 못한다.** 삭제 표지가 이미 지워져 M1 키의 레코드가 토픽에 없다.

2. **처음부터 전체를 읽어 M1이 아예 없는 상태로 만들었다.** 기존 노드만 옛 ACTIVE를 들고 있었다.

3. **값은 압축 후에도 최신 값으로 남으므로 늦게 와도 받는다.** 대신 토픽에 해지 가맹점이 계속 쌓인다.

4. **소비자의 정지 기간과 `delete.retention.ms`를 비교한다.** 정지가 더 길었다면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 정지 기간과 무관한 증분 재개
① 문제 코드
```java
consumer.resumeFrom(committedOffset());
```
② 고친 코드
```java
Duration paused = Duration.between(lastConsumedAt(), now());
if (paused.compareTo(topicConfig.deleteRetention()) >= 0) rebuildFromBeginning();   // 표지 유실 가능
else consumer.resumeFrom(committedOffset());
```
무엇이 깨졌나: 삭제를 "일시적으로만 남는 표지"로 전달하면서, 소비자가 늦게 올 수 있다는 점을 계약에 넣지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 정지 길면 전체 재구축 | 재구축 시간 허용 | 중간 | 재구축 중 지연 | 기본 |
| 해지를 값(CLOSED)으로 발행 | 소비자가 값 해석 | 작음 | 토픽 크기 증가 | 대안 |
| 보존 시간 연장 | 디스크 | 작음 | 더 긴 정지엔 같음 | 함께 적용 |

**결론**: 삭제 표지 보존 시간보다 오래 멈춘 소비자는 증분으로 돌아오지 않는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
