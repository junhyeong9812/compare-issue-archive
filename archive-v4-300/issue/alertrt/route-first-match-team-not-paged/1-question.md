# issue/alertrt/route-first-match-team-not-paged — 라우팅 트리는 첫 일치 경로에서 멈추고 continue 없이는 다음 경로로 가지 않는다 — 질문

## 질문
1. (예측) 라우트 1: `namespace=batch` → 인프라팀, 라우트 2: `team=settle` → 정산팀. 경보 `SettleFileMissing{namespace=batch, team=settle}`은 어디로 가는가?
2. (왜) 라우트 2는 지난달 추가됐고 테스트 경보도 정산팀에 갔다. 왜 실제 경보는 안 갔나?
3. (경계) `continue: true`를 라우트 1에 두면?
4. (연결) 라우팅 변경을 어떻게 검증하나?

## 복습 기록
| 날짜 | 결과 | 틀린 질문 | 다음 복습 |
|------|------|-----------|-----------|
| | | | |
