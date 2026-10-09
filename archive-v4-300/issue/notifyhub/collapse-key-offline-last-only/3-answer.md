# issue/notifyhub/collapse-key-offline-last-only — 정답

태그: `silent-failure` · `delivery-semantics`

## 정답
<!-- 질문 1:1 대응 -->

1. **1건(10:05)이다.** 나머지 둘은 플랫폼 보관 단계에서 덮였다. 발송 응답은 3건 모두 성공이다.

2. **2.0에서 "같은 종류 알림 묶기" 기본값이 켜졌다.** 릴리스 노트에는 "알림 폭주 방지"로만 적혀 있었다.

3. **보였다.** 알림함은 서버 API로 채워진다 — 그래서 "푸시만 빠졌다"는 문의가 됐다.

4. **누락 시점의 단말 접속 상태와 같은 collapse key의 연속 발송 여부를 본다.** 오프라인 구간에 같은 키가 2건 이상이면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 알림 종류를 collapse key로 사용
① 문제 코드
```java
msg.setCollapseKey(notification.type().name());      // PAYMENT, NOTICE ...
```
② 고친 코드
```java
if (notification.type().isReplaceable())            // 잔액·진행률처럼 최신값만 의미 있는 알림
    msg.setCollapseKey(notification.type() + ":" + notification.subjectId());
// 결제·보안 알림은 collapse key 없음
```
무엇이 깨졌나: 덮어써도 되는 알림과 각각 의미가 있는 알림을 같은 키로 묶었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 결제 알림 collapse 해제 | 없음 | 작음 | 오프라인 후 알림 몰림 | 기본 |
| 대상 단위 collapse key | 대상 id | 작음 | 키 설계 실수 | 갱신형 알림 |

**결론**: collapse key는 "최신값만 의미 있는 알림"에만 쓰고, 업그레이드 시 기본값 변경을 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
