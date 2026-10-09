# issue/replwatch/inactive-slot-wal-disk-full — 정답

태그: `capacity` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **계속 쌓인다.** 슬롯의 `restart_lsn` 이후 WAL을 지울 수 없어 11일치 410GB가 남았다.

2. **야간 정산 적재가 WAL을 평소의 6배로 만들었기 때문이다.** 80% 경보는 업무 시간 기준 증가 속도로 잡은 여유였다.

3. **상한을 넘은 슬롯은 무효화되고 WAL은 지워진다.** 디스크는 지키지만 그 소비자(살아 있는 CDC라면)는 재동기화가 필요하다.

4. **폐기 체크리스트에 `replwatch slot drop`**, 그리고 `active=f`이면서 보존 WAL 50GB 초과 시 경보.

## 문제 구조 (추상화 코드)

### 변형 A — 커넥터만 내리고 슬롯을 남김
① 문제 코드
```sh
kubectl delete deploy cdc-ledger
```
② 고친 코드
```sh
kubectl delete deploy cdc-ledger
replwatch slot drop cdc_ledger
replwatch slots --inactive   # 비어 있는지 확인
```
무엇이 깨졌나: 슬롯의 수명이 소비자 프로세스와 따로라는 점이 폐기 절차에 없었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 폐기 절차에 slot drop | 절차 준수 | 작음 | 사람 누락 | 기본 |
| max_slot_wal_keep_size 200GB | 소비자 재동기화 가능 | 작음 | 느린 소비자 무효화 | 안전망 |
| 비활성 슬롯 경보 | 메트릭 수집 | 작음 | 경보 소음 | 기본 |

**결론**: 절차와 경보를 같이 두고, 상한은 마지막 안전망으로만 쓴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
