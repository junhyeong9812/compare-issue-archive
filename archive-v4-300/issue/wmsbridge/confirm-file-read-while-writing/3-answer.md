# issue/wmsbridge/confirm-file-read-while-writing — 정답

태그: `race-condition` · `file-integration` · `partial-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **앞부분 1,388줄이다.**\
폴러가 집은 시점까지 쓰인 줄만 처리됐다.

2. **파일이 처음부터 최종 이름으로 생겨 "다 썼다"는 신호가 없었다.**

3. **평소엔 쓰기 시간이 10초 남짓이라 폴링 시점과 겹치는 일이 드물었다.**

4. **처리 완료 기록이 파일명 단위였다.**\
나중에 파일이 커져도 이미 완료된 이름이라 무시했다.

5. **임시 이름 → 완료 후 이름 변경, 파일 끝 트레일러(줄 수)와 처리 줄 수 대조, 불일치면 회차 실패 경보.**\
남은 752줄은 수동 재처리했다.

## 문제 구조 (추상화 코드)

### 변형 A — 완료 신호 없는 파일 공유
① 문제 코드
```python
for f in new_files(inbox):
    process(f); done.add(f.name)
```
② 고친 코드
```python
for f in new_files(inbox, suffix=".done"):          # WMS 가 다 쓴 뒤 이름 변경
    rows = read(f)
    if rows.trailer_count != len(rows.body):
        raise IncompleteFile(f.name)
    process(rows); done.add(f.name)
```
무엇이 깨졌나: 파일의 존재를 완성으로 간주했다 — 쓰기 끝을 알리는 계약이 없었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 임시 이름 후 변경 | WMS 수정 | 중간 | 벤더 일정 | 기본 |
| 트레일러 줄 수 대조 | 파일 형식 합의 | 작음 | 트레일러 누락 | 기본 |
| 파일 크기 안정 대기 | — | 작음 | 느린 쓰기에서 오판 | 임시 |

**결론**: 파일 연동은 "다 썼다"는 신호와 줄 수 대조를 함께 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
