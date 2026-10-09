# issue/ptsctl/vendor-outage-window-batch-no-retry — 정답

태그: `vendor-notice` · `partial-failure` · `retry`

## 정답
<!-- 질문 1:1 대응 -->

1. **02:30 ~ 03:10 구간에 몰려 있었다.**\
그 구간 요청 대부분이 벤더 503으로 실패했다.

2. **공지가 제휴팀 메일로만 와서 배치 담당에게 전달되지 않았다.**

3. **장애 구간에 보낸 요청만 실패했다.**\
앞뒤 구간은 정상 처리됐다.

4. **배치가 실패를 "다음 회원으로 넘어감"으로만 처리했다.**\
결과 요약의 실패 건수가 종료 상태에 반영되지 않았다.

5. **실패 로그에 해당 회원이 있는지와 요청 시각부터 본다.**\
실패 로그에 있고 시각이 한 구간에 몰리면 가용성 문제다. 성공 응답을 받았는데 잔액이 없다면 다른 원인을 본다.

## 문제 구조 (추상화 코드)

### 변형 A — 실패 건수를 종료 상태에 반영하지 않음
① 문제 코드
```python
for m in members:
    try: earn(m)
    except VendorError: failed.append(m)
log.info("완료 (실패 %d)", len(failed))
sys.exit(0)
```
② 고친 코드
```python
for m in members:
    try: earn(m)
    except VendorError: failed.append(m)
retry_queue.extend(failed, delay=timedelta(minutes=30))
sys.exit(0 if not failed else 2)      # 2 = 부분 실패 → 경보
```
무엇이 깨졌나: 부분 실패를 성공으로 접어 1만 명 넘는 누락이 하루 동안 보이지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 실패 건 지연 재시도 | 재시도 큐 | 작음 | 장애 장기화 시 재시도 소진 | 기본 |
| 부분 실패 종료 코드 | 스케줄러 경보 연동 | 작음 | 경보 피로 | 기본 |
| 벤더 공지 수신자 확대 | 연락처 관리 | 작음 | 담당 변경 시 누락 | 병행 |

**결론**: 부분 실패는 실패다 — 다시 보내고, 종료 상태로 알린다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
