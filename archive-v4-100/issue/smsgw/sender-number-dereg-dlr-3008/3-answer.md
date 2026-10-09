# issue/smsgw/sender-number-dereg-dlr-3008 — 정답

태그: `silent-failure` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **응답은 `0000`(접수)이고 고객은 받지 못한다.** `smsgw`는 수신번호·본문 형식만 보고 접수한다.\
발신번호 등록 여부는 통신사 중계 단계에서 확인되고, 실패는 수 초~수 분 뒤 DLR 콜백의 `report=3008`로 온다.

2. **성공률을 접수 응답으로 계산했기 때문이다.** DLR 수신기는 `3008`을 매핑 표에 없는 코드로 보고 DEBUG 로그만 남기고 버렸다.\
고객센터에 "인증 문자가 안 온다" 문의가 하루 140건 넘게 쌓이고서야 원인 추적이 시작됐다.

3. **실패했다.** 대체발송도 같은 발신번호 설정을 썼다. 반면 마케팅 문자는 별도 등록 번호(02 국번)를 써서 정상이었다 — 그래서 "일부 문자만 안 간다"로 보였다.

4. **해당 msgId의 DLR 코드를 본다.** `smsgw report <msgId>`가 `3008`이면 발신번호 문제로 확정된다. 리포트 자체가 없으면 통신사 지연·수신기 장애 쪽을 본다.

## 문제 구조 (추상화 코드)

### 변형 A — 접수 응답을 발송 결과로 기록함
① 문제 코드
```java
SendResult r = smsgw.send(from, to, body);
if ("0000".equals(r.result())) authLog.recordDelivered(r.msgId());   // 접수 = 성공으로 기록
// DLR 수신기: 매핑에 없는 코드는 무시
```
② 고친 코드
```java
SendResult r = smsgw.send(from, to, body);
authLog.recordAccepted(r.msgId());                                 // 접수만 기록
// DLR 수신기: 모든 코드를 저장하고, 미분류 코드는 경보
void onReport(Dlr d) {
    authLog.applyReport(d.msgId(), d.code());
    if (!ReportCode.known(d.code())) alert.warn("unknown DLR " + d.code());
}
```
무엇이 깨졌나: 비동기 전달 시스템의 접수 응답을 최종 결과로 썼고, 결과 채널(DLR)의 낯선 코드를 버렸다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 발송 성공률을 DLR 기준으로 재정의 | DLR 콜백 수신 | 지표 지연(수 분) | DLR 지연 시 일시 저하 | 기본 |
| 번호 교체 체크리스트(등록 확인 후 해지) | 통신사 등록 조회 | 작음 | 수작업 누락 | 번호 변경 작업 |

**결론**: 결과는 DLR로 판정하고, 낯선 코드는 버리지 말고 경보로 올린다. 발신번호 교체는 등록 완료 확인이 먼저다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
