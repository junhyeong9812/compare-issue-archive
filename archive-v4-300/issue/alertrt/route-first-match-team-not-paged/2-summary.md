# issue/alertrt/route-first-match-team-not-paged — 정리 (힌트)

태그: `config` · `observability`

## 전체 흐름

```
route
 ├ namespace=batch → infra   (continue 없음 → 여기서 종료)
 └ team=settle     → settle
SettleFileMissing{namespace=batch, team=settle} → infra 만
테스트 경보: namespace 라벨 없음 → settle
```

## 핵심 문장

- 라우팅은 **위에서부터 첫 일치**에서 멈춘다 — `continue: true`가 있어야 다음 형제로 간다.
- 테스트 경보에 실제 라벨(`namespace`)이 없어 다른 경로를 탔다.
- `alertrt routes test --labels ...`로 실제 라벨 조합을 검증한다.
- 팀 라우트를 인프라 일반 라우트보다 위에 둔다.
