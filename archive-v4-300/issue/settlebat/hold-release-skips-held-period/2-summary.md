# issue/settlebat/hold-release-skips-held-period — 정리 (힌트)

태그: `data-meaning` · `state-transition`

## 전체 흐름

```
가맹점 보류 2/10 ~ 3/05 (서류 미비)
정산 대상 선정: merchant.st = ACTIVE AND sale_date >= merchant.status_changed_at
      │  해제 시 status_changed_at = 3/05 로 갱신
      ▼
2/10~3/04 매출: 대상 밖 → 정산 누락, 매출은 '정산 대기'로 남음
```

## 핵심 문장

- 정산 대상 조건이 **가맹점 상태 변경 시각**을 매출 하한으로 썼다 — 원래는 신규 가맹점 첫 정산용이었다.
- 보류 해제도 상태 변경이라 하한이 해제 시각으로 밀렸다.
- 교정: 대상 = '정산 대기' 매출 전부(가맹점 ACTIVE), 하한 조건 삭제.
