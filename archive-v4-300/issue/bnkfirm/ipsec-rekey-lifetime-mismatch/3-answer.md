# issue/bnkfirm/ipsec-rekey-lifetime-mismatch — 정답

태그: `network` · `environment-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **양쪽 수명이 다르고 재협상 시작 측이 번갈아 바뀌어, 충돌이 날 때만 끊겼다.**

2. **상대 기관과 합의한 IPsec 파라미터 표(수명·암호·DH 그룹) 대조.**

3. **타임아웃 건은 결과 조회 전문으로 전부 확정했다.** 이중 이체는 없었다.

## 문제 구조 (추상화 코드)

① 문제 코드
```text
crypto ipsec sa lifetime: (default 28800)
```
② 고친 코드
```text
crypto ipsec sa lifetime seconds 3600     # 은행 합의값
monitor tunnel bank-firm interval 10 alert-on-down 30s
```
무엇이 깨졌나: 상대와 맞춰야 하는 네트워크 파라미터가 기본값으로 바뀌었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 합의 파라미터 일치 + 터널 감시 | 작음 | 없음 | 기본 |
| 재시도 강화 | 작음 | 원인 잔존 | 선택하지 않음 |

**결론**: 상대 기관과의 네트워크 파라미터는 대조표로 관리한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
