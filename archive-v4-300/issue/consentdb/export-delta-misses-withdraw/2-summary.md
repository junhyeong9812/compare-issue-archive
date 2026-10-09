# issue/consentdb/export-delta-misses-withdraw — 정리 (힌트)

태그: `sync` · `privacy`

## 전체 흐름

```
동의 행: agreed_at | withdrawn_at | updated_at(없음)
증분 추출: WHERE agreed_at > :last   → 신규 동의만
철회: withdrawn_at 채움, agreed_at 그대로 → 증분에 안 잡힘
제휴사: 증분을 '추가'로만 반영 → 철회자가 명단에 남음
월 전체 추출: 제휴사가 '덮어쓰기'가 아니라 '병합' → 역시 남음
```

## 핵심 문장

- 증분 기준이 **`agreed_at`**이라 철회(`withdrawn_at` 변경)가 잡히지 않는다.
- 제휴사는 증분을 추가로만, 월 전체 추출도 **병합**으로 반영해 철회자가 계속 남았다.
- 증분 기준은 **행이 바뀐 시각(`updated_at`)**이어야 하고, 철회는 명시적 삭제 레코드로 보낸다.
- 제휴사 명단과 우리 명단의 차집합을 월 1회 대사한다.
