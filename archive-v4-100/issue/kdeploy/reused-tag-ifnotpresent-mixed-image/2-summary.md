# issue/kdeploy/reused-tag-ifnotpresent-mixed-image — 정리 (힌트)

태그: `environment-drift` · `contract-drift`

## 전체 흐름

```
settle:2.8.1 (구, 반올림 HALF_DOWN) ── 노드 1~7 캐시 보유
settle:2.8.1 (신, HALF_UP)          ── 레지스트리
롤아웃 → 노드 1~7: 캐시 이미지 사용 / 노드 8~12: 새로 풀
정산 금액: 파드별 1원 차이 → 일 대사 차액 4,210원
```

## 핵심 문장

- 태그는 **가변 이름**이다 — 같은 태그를 다시 올려도 노드 캐시는 바뀌지 않는다.
- `IfNotPresent`는 이름만 보고 캐시를 쓴다 — 배포 도구는 성공으로 기록한다.
- 실제 실행 이미지는 파드 상태의 `imageID`(digest)로만 구분된다.
- 배포는 **digest 고정**, 레지스트리는 태그 덮어쓰기 금지(immutable)로 막는다.
