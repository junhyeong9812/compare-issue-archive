# issue/amlscan/str-file-bom-reject — 정답

태그: `silent-failure` · `encoding` · `environment-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **접수하지 않는다.** 업로드는 성공해서 `SUBMITTED`가 나오지만, FIU 일괄 수집이 첫 줄 헤더를 `HDR`로 찾지 못해 파일 전체를 반려한다.
   > **BOM(바이트 순서 표식)** — UTF-8 파일 맨 앞에 붙는 보이지 않는 3바이트 표식. 일부 수신 시스템은 이를 데이터로 읽는다.

2. **보고서 생성 도구가 3월 4일 4.0으로 올라가면서 텍스트 내보내기 기본값이 'UTF-8(BOM 포함)'이 됐다.** amlscan 설정은 그대로였다.

3. **파일 전체다.** 반려는 다음 날 새벽 `RJT_S-77120.txt`(사유 F02)로만 오고, amlscan이 결과 파일을 가져와야 상태에 반영된다.

4. **결과 파일 수거가 주 1회 배치였기 때문이다.** 3월 11~12일 제출 3건이 `SUBMITTED`로 남았고, FIU 담당자 전화(3월 13일)로 알았다.

5. **제출 전**: 첫 3바이트가 `EF BB BF`이면 제거하거나 제출을 멈춘다.\
**제출 다음 날**: 결과 파일을 매일 수거해 `ACC`(접수) 여부를 제출번호별로 대조한다.

## 문제 구조 (추상화 코드)

### 변형 A — 생성기 출력을 그대로 제출
① 문제 코드
```python
path = report_tool.export("STR", day)          # 4.0 부터 BOM 포함
out = run(["amlscan", "submit", "--kind", "STR", path])
log.info("제출 완료 %s", parse_id(out))          # SUBMITTED 를 완료로 기록
```
② 고친 코드
```python
raw = open(path, "rb").read()
if raw.startswith(b"\xef\xbb\xbf"):
    open(path, "wb").write(raw[3:])              # BOM 제거
sid = parse_id(run(["amlscan", "submit", "--kind", "STR", path]))
pending_ack.add(sid, due=next_business_day())    # 다음 날 ACC 확인 대상
```
무엇이 깨졌나: 업로드 응답을 접수로 보고, 수신 측 형식 규칙(첫 바이트)을 앞단 도구 기본값에 맡겼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 제출 전 BOM 제거·헤더 검사 | 파일 접근 | 작음 | 다른 형식 변경은 못 잡음 | 기본 |
| 결과 파일 매일 대조 | 수거 배치 | 작음 | 하루 지연 | 안전망 |

**결론**: 형식 검사는 제출 직전에, 접수 판정은 결과 파일로 한다. 앞단 도구 설정은 바뀔 수 있다고 본다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
