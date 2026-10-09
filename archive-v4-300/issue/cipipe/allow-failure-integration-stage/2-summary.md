# issue/cipipe/allow-failure-integration-stage — 정리 (힌트)

태그: `ci` · `silent-failure`

## 전체 흐름

```
unit ✔ → integration ✖ (allow_failure) → 파이프라인 '통과(경고)'
머지 규칙: 파이프라인 통과 필요 → 충족
2주간 실패 지속 → 환불 금액 계산 회귀 배포
```

## 핵심 문장

- `allow_failure`는 그 단계 실패를 **파이프라인 실패로 올리지 않는다** — 머지·배포 게이트가 열린다.
- 외부 샌드박스가 불안정하던 시기에 임시로 넣고 되돌리지 않았다.
- 경고 표시는 매번 떠 있어 무시됐다.
- 불안정 테스트는 격리(quarantine) 목록으로 관리하고, 나머지는 반드시 막는다.
