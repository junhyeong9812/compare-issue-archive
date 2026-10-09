# issue/artihub/cleanup-unused-30d-deleted-prod-image — 정리 (힌트)

태그: `deploy` · `retention`

## 전체 흐름

```
노드 캐시에 이미지 있음 → 레지스트리 다운로드 0회 (넉 달)
정리 규칙: last_downloaded > 30d → 삭제
노드 교체 → 새 노드 pull → 404 → ImagePullBackOff
```

## 핵심 문장

- '사용 중'의 기준이 **다운로드 기록**이었다 — 노드 캐시로 도는 이미지는 다운로드가 없다.
- 삭제는 노드가 바뀌는 순간에야 드러난다.
- 롤백용 이미지도 같은 규칙으로 지워져 있었다.
- 정리 전 클러스터의 **실행 중 이미지 목록**을 제외 목록으로 넣는다.
