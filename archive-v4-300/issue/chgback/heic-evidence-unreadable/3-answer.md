# issue/chgback/heic-evidence-unreadable — 정답

태그: `contract-drift` · `file-format`

## 정답
<!-- 질문 1:1 대응 -->

1. **SUBMITTED를 받고, 심사에서 증빙 없음으로 기각된다.**

2. **이전 앱은 업로드 때 JPG로 압축했다.**

3. **재심 기한(기각 후 10일) 안이면 가능하다.** 기한 내 41건을 재제출했다.

## 문제 구조 (추상화 코드)

① 문제 코드
```python
portal.submit(dispute_id, files=uploads)
```
② 고친 코드
```python
files = [to_jpeg(f) if f.mime == 'image/heic' else f for f in uploads]
assert all(f.mime in ALLOWED for f in files)
portal.submit(dispute_id, files=files)
```
무엇이 깨졌나: 상대가 받는 것과 읽을 수 있는 것이 달랐다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 변환 + 허용 형식 검사 | 작음 | 변환 실패 → 업로드 오류(보이는 실패) | 기본 |
| 앱에서 HEIC 금지 | 작음 | 가맹점 불편 | 보조 |

**결론**: 외부로 내보내는 첨부는 상대가 읽을 수 있는 형식으로 맞춘다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
