# issue/tplsvc/locale-missing-key-empty-string — 정리 (힌트)

태그: `i18n` · `silent-failure`

## 전체 흐름

```
템플릿 pay.done: ko = "{{amount_label}} {{amount}}원 결제 완료"  en = "Payment of {{amount_fmt}} completed"
새 변수 amount_fmt 추가 → ko 번역 파일만 갱신, en 은 변수 정의 누락
tplsvc: en 렌더 시 amount_fmt 미정의 → "" (기본 동작) → "Payment of  completed"
```

## 핵심 문장

- `tplsvc`는 누락 변수를 **빈 문자열**로 치환한다 — 오류도 기본 언어 대체도 없다.
- 언어 설정이 영어인 고객(약 2%)만 영향을 받아 "일부 고객"으로 보였다.
- 번역 파일 검사가 키 존재만 봤고 변수 목록 일치는 보지 않았다.
- 대처: 누락 변수 시 렌더 실패(엄격 모드) + 기본 언어 대체, CI에서 언어별 변수 집합 비교.
