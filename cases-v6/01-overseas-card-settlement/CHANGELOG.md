# CHANGELOG — overseas-card-settlement

## 2026-10-06
- settle: 휴장일 정산 환율이 다시 `--src 3` 재조회 값으로 나오게 수정 (응답 끝 `~N` 꼬리표를 버리던 파서, 10월 첫째 주 명세 불일치)

## 2026-05-12
- board: 앱 환율판에 CNY 추가

## 2025-11-21
- export: 회계 CSV에서 `rate` 칸 제거 (OPS-2291)
- fxq: 3.4.1 대응 — 응답 형식 변경 없음

## 2025-09-02
- settle: 휴장일(`~N` 꼬리표) 정산 환율은 같은 날짜 `--src 3` 재조회 (벤더 부속 계약 체결)

## 2025-08-11
- reconcile: 파트너 명세의 `count` 칸도 대사

## 2025-06-30
- settle: 승인 시각 UTC → KST 일자 변환을 `BusinessDateResolver`로 분리
- fee: 일자·파트너 합계 기준 원 미만 절사 (파트너 약정 별표 2)
