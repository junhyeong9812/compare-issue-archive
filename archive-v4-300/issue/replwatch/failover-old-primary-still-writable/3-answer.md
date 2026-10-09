# issue/replwatch/failover-old-primary-still-writable — 정답

태그: `split-brain` · `data-loss`

## 정답
<!-- 질문 1:1 대응 -->

1. **옛 주 DB에 기록되고 새 주 DB에는 없다.** 30분간 인스턴스 #3으로 간 주문 212건이 그랬다.

2. **`status`는 replwatch가 관리하는 역할만 보여 준다.** 옛 서버는 관리 대상에서 빠졌을 뿐 PostgreSQL은 read-write로 떠 있었다.\
런북에 `fence` 단계가 없었다.

3. **새 주 DB로 갔을 것이다.** 이번엔 커넥션 최대 수명이 2시간이라 30분간 옛 커넥션이 유지됐다.

4. **옛 주 DB에서 장애 조치 시각 이후 `xmin`이 큰 주문 행을 뽑아** 새 주 DB와 주문번호로 비교했다.\
없는 212건을 옮기고, 그 사이 결제 승인 기록과 대사해 금액을 확인했다.

## 문제 구조 (추상화 코드)

### 변형 A — 승격만 하고 옛 주 DB를 막지 않음
① 문제 코드
```sh
replwatch promote replica-2
vip move pay-db replica-2
```
② 고친 코드
```sh
replwatch fence old-primary      # default_transaction_read_only + 기존 세션 종료
replwatch promote replica-2
vip move pay-db replica-2
```
무엇이 깨졌나: 장애 조치를 '새 쪽 올리기'로만 보고 '옛 쪽 막기'를 빠뜨렸다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| fence 선행 | 옛 서버 접근 가능 | 작음 | 옛 서버 접근 불가 시 실행 불가 | 기본 |
| 커넥션 최대 수명 단축 | 풀 설정 | 재연결 비용 | 노출 시간만 줄어듦 | 보조 |
| STONITH(전원 차단) | IPMI 권한 | 중간 | 오작동 시 정상 서버 차단 | 접근 불가 상황 |

**결론**: 런북을 fence → promote → VIP 순서로 고치고, fence가 실패하면 전원 차단으로 넘어간다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
