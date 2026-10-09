# issue/remitx/sanctions-screening-hold — 정답

태그: `async-status` · `compliance`

## 정답
<!-- 질문 1:1 대응 -->

1. **은행 내부 심사에서 멈췄다.** SWIFT로 나가지 않았다.

2. **같은 수취인이면 다시 보류된다.** 요청 서류(거래 목적·수취인 신원)를 내는 것이 길이다.

3. **UETR 추적 상태(ACSC 등) 또는 은행 발송 확인.**

## 문제 구조 (추상화 코드)

① 문제 코드
```python
r = remitx.send(...); remit.set_done(r.id)
```
② 고친 코드
```python
r = remitx.send(...); remit.set_sent(r.id)
# 추적 배치: inquiry(id).compliance == 'REVIEW' → 경보, uetr 상태 미진행 24h → 경보
```
무엇이 깨졌나: 은행 수신 응답을 송금 완료로 썼다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 추적 상태 기반 완료 + 보류 경보 | 중간 | 없음 | 기본 |
| 수취인 사전 심사 | 중간 | 오탐 | 보조 |

**결론**: 송금 완료는 네트워크 추적 상태로 판정한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
