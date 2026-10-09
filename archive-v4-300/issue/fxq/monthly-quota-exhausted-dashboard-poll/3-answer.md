# issue/fxq/monthly-quota-exhausted-dashboard-poll — 정답

태그: `resource-bounding` · `silent-failure` · `cost`

## 정답
<!-- 질문 1:1 대응 -->

1. **통화 10~14에 대해 벤더의 한도 초과 응답과 비정상 종료가 남아 있었다.**\
배치 로그에는 통화별로 오류 한 줄씩이 찍혔지만 경보 대상이 아니었다. 리포트 생성 단계는 성공으로 기록됐다.

2. **3월 3일 배포된 실시간 대시보드가 같은 API 키로 1분마다 14종을 조회했기 때문이다.**\
하루 2만 건 남짓이 더해져 27일 새벽에 월 한도에 닿았다. 대시보드 담당 팀은 한도제 계약이라는 사실을 몰랐다.

3. **사내 래퍼가 통화별 조회를 try/except로 감싸고 실패 통화를 None으로 넘겼기 때문이다.**\
리포트 템플릿은 None을 빈칸으로 그렸고, 완료 판정은 "파일 생성 여부"만 봤다.

4. **값의 존재부터 본다.** 이 사건은 값이 아예 없고 종료 코드가 비정상이었다.\
"값은 나왔지만 틀린" 사고는 값이 있고 정상 종료라 행 수·빈칸 검사로는 안 보인다 — 두 유형은 확인 방법이 다르다.

## 문제 구조 (추상화 코드)

### 변형 A — 통화별 실패를 삼키고 리포트를 완성함
① 문제 코드
```python
rates = {}
for ccy in CURRENCIES:
    try:
        rates[ccy] = fxq_rate(ccy, day)
    except FxqError:
        rates[ccy] = None          # 빈칸으로 넘어감
render_report(rates)               # "성공"
```
② 고친 코드
```python
rates, failed = {}, []
for ccy in CURRENCIES:
    try:
        rates[ccy] = fxq_rate(ccy, day)
    except FxqError as e:
        failed.append((ccy, str(e)))
if failed:
    raise ReportIncomplete(failed)  # 빈칸 리포트는 만들지 않는다
render_report(rates)
```
무엇이 깨졌나: 부분 실패를 성공으로 접었다 — 한도 소진이라는 운영 신호가 리포트 빈칸으로 바뀌어 사라졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 용도별 API 키 분리 | 벤더가 키 추가 발급 허용 | 키 관리 1건 추가 | 키별 한도 재분배 실수 | 기본 |
| 대시보드 캐시 5분 | 실시간성 요구 낮음 | 작음 | 캐시 서버 장애 시 폴링 폭주 | 기본 |

**결론**: 평가·정산용 키는 분리해 보호하고, 리포트는 빈칸 하나라도 있으면 실패로 올린다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
