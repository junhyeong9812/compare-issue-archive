# issue/flowctl/heartbeat-timeout-restart-loop — 정답

태그: `performance` · `timeout`

## 정답
<!-- 질문 1:1 대응 -->

1. **30초 뒤 실패로 처리되고 처음부터 다시 시작한다.** 영원히 끝나지 않는다.

2. **엔진이 실패로 본 이전 시도도 워커에서는 계속 돌고 있었다.** 취소 신호를 확인하지 않았다.

3. **워커가 죽었을 때 2시간이 지나야 감지된다.** 하트비트를 보내는 쪽이 맞다.

## 문제 구조 (추상화 코드)

### 변형 A — 하트비트 없는 장시간 단계
① 문제 코드
```java
void renderStatement(Req r) { for (Page p : pages(r)) render(p); }
```
② 고친 코드
```java
void renderStatement(Req r) {
    int from = Activity.getHeartbeatDetails(Integer.class).orElse(0);
    for (int i = from; i < total(r); i++) {
        render(page(r, i));
        if (i % 1000 == 0) Activity.heartbeat(i);   // 취소 시 예외로 중단
    }
}
```
무엇이 깨졌나: 단계 길이가 입력 크기에 비례하는데 생존 신호 규약을 지키지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 하트비트 + 진행 위치 | 엔진 지원 | 작음 | 없음 | 기본 |
| 큰 입력은 분할 단계 | 분할 설계 | 중간 | 복잡도 | 수십만 쪽 |

**결론**: 긴 단계는 하트비트로 진행 위치를 남기고, 취소를 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
