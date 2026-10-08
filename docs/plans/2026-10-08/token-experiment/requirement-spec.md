# 요구사항 명세서 (requirement-spec)

> 작성일: 2026-10-08 · 작업 폴더: `docs/plans/2026-10-08/token-experiment/`

---

## 0. 요구사항 원문 (인터뷰 기록)

- 원문(최초): "…해당 issue문서를 보고 문제를 해결하는 방안이랑, 에이전트가 자료를 조사해서 직접 해결하는 방식에 대한 토큰이 얼마나 덜 쓰이는지?를 비교해서 실제 issue문서를 아카이브 하는게 얼마나 도움이 되는 지 보고 싶어."
- 원문(이번): "1번은 보지 않도록 접근제어하는게 맞고 4번은 이슈를 스스로 찾게 할꺼고 3번은 3회정도, 2번에 대해서 서브에이전트만 통제하는게 맞다고 보는데, 애초에 문제 해결에 하네스 이전에 이슈문서를 보는 역할이 얼마나 차이가 생기는 지 알고 싶은거라서," / 인증 질문에 "아니 그냥 서브에이전트한테 작업을 시키면 되는거잖아?"
- Q/A:
  - B(직접 조사)는 아카이브 접근 차단. A(아카이브)는 issue/ 인덱스에서 **스스로 찾기**.
  - 반복 3회 → 10 케이스 × 2 arm × 3 = **60 실행**.
  - 통제 대상 = 실험 서브에이전트. 측정하려는 것 = 이슈 문서를 보는 것의 효과(하네스 효과 아님) → 두 arm 조건 동일, 차이는 아카이브 유무뿐.
  - 웹 검색: 두 arm 모두 허용. 모델: Sonnet. 실패 실행도 기록(성공률 비교).
  - 실행 수단: 서브에이전트 안을 거쳐 사용자 제안("claude 명령어로 작업 요청을 보내면 되는 거 아니야?")에 따라 **`claude -p` 헤드리스 + 격리 HOME**(자격 증명 파일만 복사 — OAuth 구독, `--bare`는 API 키 전용이라 미사용). 스모크 확인: 격리 HOME에서 CLAUDE.md 미인식, usage·total_cost_usd 정상 반환.

## 1. 목표·대상 (필수)

`compare-issue-archive`에서 케이스 10개를 A/B 각 3회 서브에이전트로 풀게 하고, 실행별 토큰·성공 여부를 기록해 **arm별 토큰 차이와 성공률**을 케이스 단위 표로 낸다(`results/`).

## 2. 경계·불변식 (필수)

- 두 arm의 조건은 아카이브 안내 한 단락 외 동일: `claude -p --model sonnet --output-format stream-json --verbose --max-budget-usd 2 --permission-mode bypassPermissions`(샌드박스 안이라 허용), 격리 HOME(하네스·훅·메모리·MCP 없음), 같은 작업 지시문 골격.
- 작업 공간 격리: 실행마다 `cases/NN`을 실행 폴더로 복사. **bwrap으로 `/home/jun/project` 전체를 가린다**(OS 수준 — oracle·study-note 원본 접근 불가, 두 arm 공통). A에만 `issue/` 사본을 읽기 전용으로 bind해 경로를 알려 준다.
- **접근 위반 감사**: study-note는 공개 GitHub repo라 웹으로 우회 가능 → 실행 stream-json 로그를 스크립트로 grep(`study-note`·`junhyeong9812`·`issue-archive`·`oracle`) — 걸리면 **무효** 처리 후 재실행(무효 건수 기록). 로그 원문은 메인 컨텍스트로 읽지 않는다.
- 판정: 실행 종료 후 작업 폴더에 hidden/ 덮어쓰기 → `test.sh`·`hidden.sh` 둘 다 통과 = 성공. 원본 대비 테스트 파일이 바뀌었으면 실패(변조).
- 토큰: 최종 result 이벤트의 usage(input·output·cache_creation·cache_read)·total_cost_usd·num_turns·duration_ms와 도구 호출 수를 기록. 실패·무효·예산 초과 실행도 행으로 남긴다(조용한 누락 금지).
- 동시 실행 ≤5(백그라운드 프로세스). 실행 순서는 케이스마다 A/B 교대.

## 3. 기준소스 (필수)

- 케이스·판정: `compare-issue-archive` `feat/case-build` 커밋 d4efa20의 `cases/`·`oracle/`.
- 아카이브: study-note `issue/` 현재 작업트리(사본 생성 시점 SHA 기록).

## 4. 금지영역 (필수)

- study-note 수정 금지(읽기·복사만). 타 세션의 rules-checker 변경 무접촉.
- `cases/`·`oracle/` 내용 변경 금지(실험 도중 케이스 수정 시 해당 케이스 전체 재실행 + 사용자 보고).
- 원격 push·발행 없음.

## 5. 검증 방법 (필수)

- 착수 스모크: 1개 케이스 A/B 각 1회 → (a) bwrap 안에서 claude가 인증·실행되고 `/home/jun/project`가 안 보이는지, result usage가 기록되는지 (b) 감사 grep이 의도적 위반 샘플을 잡는지(fail-open 금지) (c) 판정 스크립트가 미수정 작업폴더를 실패로, fix.patch 적용본을 성공으로 판정하는지.
- 본 실험 후: runs 행 수 = 60 + 무효 재실행 수, 케이스×arm별 3행 충족, 요약 표의 수치를 원시 행 2개로 손 검산.

## 6. stakes (필수)

- 판정: **낮음** — 로컬 실험, 불가역 없음, 잘못돼도 재실행으로 복구. 단, 집계 스크립트는 결과 해석에 직결되므로 §5 손 검산과 감사 fail-open 검사를 생략하지 않는다.

---

## 7. 자율성

- [ ] auto
- [ ] lazy

## 8. load-bearing 가정

1. bwrap(비특권) 안에서 격리 HOME의 claude -p가 인증·도구 실행(JDK·node·tsc·python)까지 정상 동작한다 — 스모크 (a).
2. 로컬 경로는 bwrap이, 웹 우회는 로그 감사가 막는다 — 스모크 (b).

## 9. task 분해

| task | 목표 | 의존 | acceptance |
|------|------|------|-----------|
| 01 | 스크립트: run-one(작업폴더·bwrap·claude -p·로그 저장) · judge(hidden 판정·변조 검사) · audit · summarize | — | 스모크 (b)(c) |
| 02 | 스모크 1케이스 A/B | 01 | 스모크 (a) |
| 03 | 본 실험 60회(동시 ≤5) | 02 | runs 행 충족 |
| 04 | 요약 표·해석(`results/summary.md`) + 커밋 | 03 | 손 검산 |
