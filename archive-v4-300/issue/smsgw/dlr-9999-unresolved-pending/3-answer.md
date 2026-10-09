# issue/smsgw/dlr-9999-unresolved-pending — 정답

태그: `silent-failure` · `state-mapping`

## 정답
<!-- 질문 1:1 대응 -->

1. **계속 `SENDING`이다.** 24시간 뒤 `9999` DLR이 왔지만 매핑 표에 없어 상태를 바꾸지 않았다.

2. **배치가 `FAILED`만 고르기 때문이다.** `SENDING`은 "아직 진행 중"으로 간주돼 제외됐다. 연휴 사흘 동안 약 2,300건이 이렇게 남았다.

3. **섞여 있다.** 표본 200건을 고객 문의·수신 확인으로 대조하니 약 60%는 도착했다. 그래서 무조건 재발송하면 중복 안내가 되고, 무시하면 누락이 된다.

4. **상태별 건 수와 마지막 DLR 코드 분포를 본다.** `SENDING`에 오래 머문 건의 마지막 코드가 `9999`면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 모르는 결과 코드를 "진행 중"으로 둠
① 문제 코드
```java
Status map(String code) {
    if (code.equals("0000")) return DELIVERED;
    if (code.startsWith("3") || code.startsWith("4")) return FAILED;
    return null;                                  // null → 상태 유지
}
```
② 고친 코드
```java
Status map(String code) {
    if (code.equals("0000")) return DELIVERED;
    if (code.equals("9999")) return UNKNOWN;      // 결과 미수신 확정
    if (code.startsWith("3") || code.startsWith("4")) return FAILED;
    alert.warn("unmapped DLR " + code);
    return UNKNOWN;
}
```
무엇이 깨졌나: 종결 상태를 "진행 중"으로 오인했다 — 상태 기계에 "모름"이라는 종결 상태가 없었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| UNKNOWN 상태 + 재발송 정책 분리 | 업무별 중요도 정의 | 상태 추가 | 중복 안내 가능 | 기본 |
| UNKNOWN 은 앱 푸시로 보완 | 앱 설치 고객 | 채널 추가 | 미설치 고객 누락 | 배송 안내 |
| 무조건 재발송 | 없음 | 비용 2배 | 중복 수신 민원 | 인증번호처럼 짧은 수명 |

**결론**: 게이트웨이의 종결 코드는 전부 우리 상태로 옮기고, "모름"은 별도 상태로 다룬다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
