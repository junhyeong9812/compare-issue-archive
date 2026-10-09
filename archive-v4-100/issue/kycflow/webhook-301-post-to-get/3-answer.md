# issue/kycflow/webhook-301-post-to-get — 정답

태그: `silent-failure` · `environment-drift` · `protocol`

## 정답
<!-- 질문 1:1 대응 -->

1. **본문 없는 GET을 받는다.** kycflow 클라이언트가 301을 따라가며 메서드를 바꿨다. 핸들러의 GET 분기는 헬스체크라 `200 ok`를 줬다.

2. **최종 응답이 200이었기 때문이다.** kycflow는 리다이렉트 체인의 마지막 상태 코드만 기록한다. 재시도도 하지 않았다.

3. **307/308이었다면 POST와 본문이 유지돼 정상 처리됐다.** 인그레스 기본 리다이렉트가 301이었다.

4. **리다이렉트 전 이미 결과를 받은 건은 영향이 없었다.** 7월 16일 이후 완료된 심사 212건만 `REVIEWING`에 남았고, 가맹점 정산 보류 문의(7월 18일)로 발견됐다.

5. **수신 로그에 POST 200이 있으면 처리 쪽, 없거나 GET만 있으면 수신 쪽이다.** 이번엔 같은 시각 GET 200만 있었다. 누락분은 `kycflow result <case>`로 다시 가져와 반영했다.

## 문제 구조 (추상화 코드)

### 변형 A — 웹훅 경로가 GET에 200
① 문제 코드
```java
@RequestMapping("/kyc/done")            // 메서드 제한 없음
public ResponseEntity<?> done(@RequestBody(required = false) KycResult r) {
    if (r == null) return ResponseEntity.ok("ok");   // 헬스체크 겸용
    service.apply(r);
    return ResponseEntity.ok().build();
}
```
② 고친 코드
```java
@PostMapping("/kyc/done")               // GET 등은 405
public ResponseEntity<?> done(@RequestBody @Valid KycResult r) {
    service.apply(r);
    return ResponseEntity.ok().build();
}
// 헬스체크는 /kyc/health 로 분리, 웹훅 주소는 https 로 재등록
```
무엇이 깨졌나: 리다이렉트가 요청의 의미(메서드·본문)를 바꿨고, 수신 경로가 그 변형된 요청에 성공으로 답했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| https 주소 재등록 | 벤더 콘솔 권한 | 작음 | 다른 리다이렉트 생기면 재발 | 기본 |
| GET 405 · 메서드 고정 | 핸들러 수정 | 작음 | 없음 | 함께 |
| REVIEWING 30분 초과 건 폴링 | result API | 중간 | 호출량 | 안전망 |

**결론**: 웹훅은 실패가 벤더에게 실패로 보이게 만든다. 미수신 보충 폴링을 함께 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
