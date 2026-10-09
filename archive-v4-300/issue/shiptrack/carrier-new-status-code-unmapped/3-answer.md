# issue/shiptrack/carrier-new-status-code-unmapped — 정답

태그: `integration` · `contract-drift` · `partial-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **무인 보관함으로 배송된 주문이었다.**

2. **공지가 택배사 개발자 포털에만 올라왔고 구독자가 없었다.**

3. **다른 주문은 기존 "배송 완료" 코드를 받았다.**

4. **매핑에 없는 코드는 DEBUG 로그만 남기고 무시했다.**

5. **모르는 코드는 "확인 필요" 큐와 경보로, 신설 코드 매핑 추가, 오래 멈춘 배송 주문 일일 점검.**

## 문제 구조 (추상화 코드)

### 변형 A — 모르는 외부 코드를 버림
① 문제 코드
```java
Status s = MAPPING.get(code);
if (s == null) { log.debug("unknown {}", code); return; }
```
② 고친 코드
```java
Status s = MAPPING.get(code);
if (s == null) {
    unknownQueue.push(order, code);         // 확인 필요 + 경보
    metrics.incr("shiptrack.unknown_code", code);
    return;
}
```
무엇이 깨졌나: 외부 코드 체계가 늘어날 수 있다는 점을 처리 경로에 넣지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 모르는 코드 경보 큐 | 운영 확인 인력 | 작음 | 큐 방치 | 기본 |
| 장기 정체 주문 점검 | 일 배치 | 작음 | 기준일 설정 | 기본 |
| 모르는 코드는 "배송 완료"로 간주 | — | 작음 | 반송·분실을 완료로 오인 | 선택하지 않은 방법 |

**결론**: 외부 코드는 모르는 값이 오는 날을 전제로 처리한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
