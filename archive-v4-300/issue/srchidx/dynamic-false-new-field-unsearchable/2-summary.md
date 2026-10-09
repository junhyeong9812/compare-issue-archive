# issue/srchidx/dynamic-false-new-field-unsearchable — 정리 (힌트)

태그: `contract-drift` · `silent-failure`

## 전체 흐름

```
색인기 → { ..., "biz_type": "CORP" } → merchants (dynamic:false)
   ├ _source 에는 저장됨 → 상세 화면에는 보임
   └ 매핑에 없음 → 역색인 없음 → term 검색 0건
```

## 핵심 문장

- `dynamic: false`는 모르는 필드를 **거부하지도 색인하지도 않고** `_source`에만 남긴다.
- 그래서 상세 화면엔 값이 보이고 필터 검색만 0건이다 — '데이터는 있는데 검색이 안 됨'.
- 매핑을 추가해도 **이미 들어간 문서는 다시 색인해야** 검색된다(`_update_by_query`).
- 배포 순서: 매핑 추가 → 색인기 배포 → 기존 문서 재색인. `strict`를 쓰면 누락이 오류로 드러난다.
