# issue/hookin/ack-before-persist-pod-kill — 정답

태그: `silent-failure` · `durability`

## 정답
<!-- 질문 1:1 대응 -->

1. **사라진다.** PG는 재전송하지 않고, 우리 큐에는 들어가지 않았다.

2. **파드 종료가 배포 때 몰려 있었다.** 6개월간 누락 31건이 전부 배포 시간대였다.

3. **큐 적재는 수 ms라 PG 타임아웃(10초)에 영향이 없다.** 큐 장애 시엔 500을 돌려 PG가 재전송하게 하는 쪽이 맞다.

4. **누락 건의 수신 로그(접근 로그 200)와 큐 적재 로그를 맞춰 본다.** 200은 있는데 적재가 없으면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 수신 확인 후 비동기 적재
① 문제 코드
```java
@PostMapping("/notify")
ResponseEntity<Void> notify(@RequestBody byte[] body) {
    executor.submit(() -> queue.publish(body));
    return ResponseEntity.ok().build();
}
```
② 고친 코드
```java
@PostMapping("/notify")
ResponseEntity<Void> notify(@RequestBody byte[] body) {
    queue.publishSync(body, Duration.ofSeconds(2));        // 내구 적재 확인 후 응답
    return ResponseEntity.ok().build();                     // 실패 시 예외 → 500 → PG 재전송
}
```
무엇이 깨졌나: 수신 확인(책임 인수)과 내구 저장의 순서가 뒤바뀌었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 적재 후 응답 | 큐 지연 짧음 | 작음 | 큐 장애 시 PG 재전송 | 기본 |
| graceful shutdown | 배포 설정 | 작음 | 유예 초과 | 함께 적용 |
| PG 거래 내역 일 대사 | 조회 API | 작음 | 하루 지연 | 안전망 |

**결론**: 외부에 수신 확인을 주기 전에 내구 저장을 끝내고, 일 대사로 결과를 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
