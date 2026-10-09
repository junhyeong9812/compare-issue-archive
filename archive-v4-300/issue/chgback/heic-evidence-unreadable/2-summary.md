# issue/chgback/heic-evidence-unreadable — 정리 (힌트)

태그: `contract-drift` · `file-format`

## 전체 흐름

```
가맹점 앱: 아이폰 사진 업로드 (HEIC)
chgback dispute submit → 첨부 그대로 카드사 포털 전송 → 응답 SUBMITTED
카드사 심사 뷰어: JPG·PNG·PDF 만 표시 → '증빙 없음' 판정 → 기각
```

## 핵심 문장

- 카드사 포털 API는 확장자를 검사하지 않고 받는다 — **뷰어에서만** 못 연다.
- 2025-08 가맹점 앱 업데이트로 사진 원본(HEIC) 업로드가 기본이 됐다.
- 교정: 업로드 시 JPG 변환, 제출 전 허용 형식 검사.
