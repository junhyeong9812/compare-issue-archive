# issue/ocrid/callback-underscore-header-drop — 정리 (힌트)

태그: `environment-drift` · `fail-closed` · `parser-differential`

## 전체 흐름

```
ocrid ──POST /kyc/callback  헤더 x_ocr_sig: ...──▶ 인그레스
                                                     │
            구 인그레스 (70%) ── 헤더 그대로 ─────────▶ 핸들러: 서명 OK → 200 → 고객 "확인 완료"
            신 nginx   (30%) ── underscores_in_headers off (기본)
                                 → 언더스코어 헤더 조용히 버림 ─▶ 핸들러: 서명 없음 → 401

ocrid 재시도: 1분·5분·30분 (3회) → 이후 포기, 결과는 벤더 쪽에 보관
고객 상태: 콜백만 기다림 → "진행 중" 고정

교정: 콜백 location 에 underscores_in_headers on
      + "진행 중" 10분 초과 건은 ocrid result <id> 로 폴링해 보충
```

## 핵심 문장

- nginx는 기본 설정에서 **언더스코어가 든 요청 헤더를 조용히 버린다** — 벤더 콜백의 서명 헤더 `x_ocr_sig`가 사라졌다.
- 인그레스 이전이 **카나리 30%**라 그 비율만큼만 실패했다 — "일부"는 트래픽 분할의 흔적이었다.
- 벤더는 콜백 실패 후 **3회 재시도하고 포기**한다 — 결과는 `ocrid result <id>` 조회로만 받을 수 있다.
- 서명 검증은 끄지 않는다 — 수신 경로를 고치고, 콜백만 믿지 말고 미수신 건을 폴링으로 보충한다.
- 결과 미반영이면 먼저 **콜백 수신 로그의 상태 코드**를 본다 — 4xx면 수신 쪽, 수신 200인데 미반영이면 처리 쪽이다.
