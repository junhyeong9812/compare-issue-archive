# issue/splitab/late-event-early-readout-underestimate — 정답

태그: `data-freshness` · `statistics` · `experimentation`

## 정답
<!-- 질문 1:1 대응 -->

1. **늦게 도착한 앱 이벤트가 다음 날 들어왔다.**\
종료 직후에는 B안의 앱 전환 이벤트 일부가 없었다.

2. **B안 진입점이 앱에 더 많아 앱 사용자 비중이 높았다.**

3. **웹은 실시간 전송이라 두 시점 결과가 같았다.**

4. **판독은 종료 24시간 뒤부터, 채널별 도착 완료율 표시, 조기 판독은 "잠정" 표기.**\
이번 결정은 다음 날 판독 기준으로 했다.

## 문제 구조 (추상화 코드)

### 변형 A — 데이터 완결 전 판독
① 문제 코드
```python
if now() >= test.end:
    publish(readout(test))
```
② 고친 코드
```python
if now() >= test.end + timedelta(hours=24) and arrival_ratio(test) > 0.995:
    publish(readout(test))
else:
    publish(readout(test), label="잠정")
```
무엇이 깨졌나: 데이터가 다 도착했는지 확인하지 않고 종료 시각을 판독 시각으로 썼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 판독 대기 24시간 | 결정 지연 허용 | 작음 | 결정 늦음 | 기본 |
| 도착 완료율 표시 | 업로드 시각 수집 | 작음 | — | 기본 |
| 앱 SDK 실시간 전송 | 배터리·비용 | 큼 | 사용자 불만 | 선택하지 않은 방법 |

**결론**: 판독은 종료가 아니라 데이터 도착 완료 후에 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
