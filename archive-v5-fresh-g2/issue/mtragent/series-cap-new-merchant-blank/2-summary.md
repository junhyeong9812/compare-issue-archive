# issue/mtragent/series-cap-new-merchant-blank — 정리 (힌트)

태그: `silent-failure` · `resource-bounding`

## 전체 흐름

```
결제 API ── inc(pay_approve_total{merchant_id=M, result=...}) ──▶ mtragent (프로세스당)
                                                                   │
                       시리즈 수 < 10,000 → 새 시리즈 생성 → 수집 → 대시보드·경보
                       시리즈 수 ≥ 10,000 → 새 시리즈 버림 (기존 시리즈는 정상)
                                             └ 흔적: mtragent_dropped_series_total 증가만
                                                     (로그 없음, 수집 서버 경고 없음)

기존 가맹점 = 이미 시리즈 있음 → 정상
신규 가맹점 = 상한 이후 첫 등장 → 영영 없음 → 대시보드 빈칸, 경보 조건 평가 대상 아님

교정: merchant_id 를 메트릭 라벨에서 제거 (라벨은 유한 집합만)
      가맹점별 지표는 로그·이벤트 → 집계 테이블
      mtragent_dropped_series_total > 0 경보
```

## 핵심 문장

- `mtragent`는 프로세스당 **시리즈 10,000개**를 넘으면 새 시리즈를 **오류·로그 없이 버린다** — 기존 시리즈는 계속 수집된다.
- 그래서 **나중에 등장한 라벨 값(신규 가맹점)만** 영영 안 보인다.
- 데이터가 없는 시리즈는 경보 규칙의 평가 대상도 아니다 — "경보 없음"이 "문제 없음"이 아니었다.
- 고카디널리티 값(가맹점·회원·주문 ID)은 메트릭 라벨에 넣지 않는다 — 가맹점별 지표는 이벤트·로그 집계로 푼다.
- `mtragent_dropped_series_total`을 감시한다.
