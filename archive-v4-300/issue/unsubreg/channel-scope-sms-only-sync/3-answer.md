# issue/unsubreg/channel-scope-sms-only-sync — 정답

태그: `compliance` · `data-semantics`

## 정답
<!-- 질문 1:1 대응 -->

1. **발송된다.** 우리 쪽에는 SMS 거부만 기록돼 있다.

2. **동기화는 성공으로 끝났고 행 수도 맞았다.** 값의 의미가 바뀐 것은 아무 검사도 보지 않았다.

3. **모든 광고 채널 거부다.** 이후 새 채널이 생겨도 ALL은 그 채널을 포함하도록 조회 시 해석한다.

4. **민원 고객의 벤더 원본 행과 우리 저장 값을 비교한다.** 원본에는 있는데 채널이 다르면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 열 위치 기반 파싱
① 문제 코드
```python
for row in csv.reader(f):
    phone, optout_at = row[0], row[1]
    save_optout(phone, "SMS", optout_at)
```
② 고친 코드
```python
r = csv.DictReader(f)
if set(r.fieldnames) - KNOWN_COLUMNS: raise SchemaChanged(r.fieldnames)
for row in r:
    ch = row["channel"]
    if ch not in {"SMS", "ALIMTALK", "ALL"}: raise UnknownChannel(ch)
    save_optout(row["phone"], ch, row["optout_at"])
```
무엇이 깨졌나: 외부 파일의 형식 변화를 감지하지 못하는 파서가 의미 변화를 조용히 흡수했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 헤더 기반 + 낯선 열 실패 | 벤더 헤더 제공 | 작음 | 형식 변경 시 동기화 정지 | 기본 |
| 벤더 변경 공지 구독 | 공지 채널 | 작음 | 공지 누락 | 함께 적용 |

**결론**: 규제 데이터 동기화는 형식이 바뀌면 멈추게 하고, 발송 직전 채널별 거부를 모두 본다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
