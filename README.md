# compare-issue-archive

## 결론 (1~5차, 2026-10-08 ~ 2026-10-10, 마감)

"이슈·결정 기록을 남겨 두면 헤드리스 에이전트(`claude -p`, Sonnet·Haiku)에게 도움이 되는가"를 6번의 실험, 실행 약 1,900회로 쟀다.

1. **모델이 이미 아는 일반 기술 지식은 기록 가치가 없다.** 정답률 이득이 없고 토큰만 늘었다. 모델이 좋아질수록 이 범위는 넓어진다.
2. **모델이 알 수 없는 회사·벤더 고유 동작은 토큰을 절반 안팎 줄인다.** 단 기록이 맞을 때만이다. 낡은 기록은 성공률을 떨어뜨리고 토큰도 늘린다. 에이전트 지시에 "기록의 버전과 현재 `--version`을 대조하고, 다르면 직접 확인하라"를 넣어야 한다.
3. **의사결정 기록은 토큰을 줄이지 않지만, 테스트로 드러나지 않는 결정 위반을 막는다.** 기록이 없으면 20~33%가 보이는 테스트를 통과한 채 과거 결정을 깼고, 기록(2~3줄 요약 포함)이 있으면 0%였다.
4. **미검증, 다음 프로젝트로 넘김:** 코드·파일 탐색 그래프를 미리 만들어 주면 탐색 비용이 줄고, 다음 세션이 코드베이스를 더 빨리 이해하는가.

| 차수 | 질문 | 결과 (÷B = 기록 없음 B 대비 총 토큰, 중앙값) | 근거 |
|---|---|---|---|
| 1차 | 일반 버그 카드를 스스로 찾아 쓰면? | 카드를 연 실행 0회, A÷B ≈ 1.0. 먼저 보라고 지시하면(F) 1.74, 성공률 차이 없음 | [ADR-0001](docs/adr/0001-first-experiment-and-why-it-showed-no-difference.md) |
| 2차 | 큰 코드베이스·증상과 원인 분리 | 성공 B 28 / F 28 / P 30(30회 중). F ×1.87, P ×1.35, 요약 L ×1.05~1.12 | [ADR-0002](docs/adr/0002-second-experiment-design.md) |
| 3차 | 모델이 모르는 벤더 동작 카드 | Sonnet L 0.42 / P 0.49 / F 0.53, Haiku 0.55 / 0.61 / 0.64 | [ADR-0003](docs/adr/0003-project-specific-knowledge-experiment.md) |
| 4차 | 카드가 낡았으면? 카드 300장이면? | 낡은 카드: Sonnet 성공 17~22/30, ÷B 1.08~1.27. 300장 F: 120/120 성공, ÷B 0.73~0.79 | [ADR-0004](docs/adr/0004-stale-cards-and-archive-scale.md) |
| 5차-a | 낡은 카드 방어 | 버전 대조 지시: Sonnet 90/90, ÷B 0.74. Haiku 80/90(탐침 결과를 카드 쪽으로 오독). 카드에 버전 줄만 넣기: 효과 없음 | [ADR-0005](docs/adr/0005-stale-card-defense.md) |
| 5차-b | 의사결정 기록(변경 요청 과제) | 결정 위반 B: Sonnet 10/30, Haiku 6/30. 기록 arm: 0/180. ÷B 0.88~1.21 | [ADR-0006](docs/adr/0006-decision-history-experiment.md) |

- arm: B = 기록 없음, L = 결론 요약 2~3줄, P = 정답 기록 경로, F = 아카이브 인덱스에서 스스로 찾기.
- 차수별 분석: `results*/analysis.md`. 실행 기록: `docs/measurement-log.md`.
- 아래는 1차 실험 당시의 구조 설명이다. 2차부터는 `cases-vN/`·`oracle-vN/`·`archive-vN/`·`results-vN*/`를 차수별로 따로 둔다.

study-note `issue/` 카드(실전 이슈 아카이브)가 버그 수정에 얼마나 도움이 되는지 재기 위한 실험장.
같은 버그를 두 방식으로 고치게 하고 `claude -p --output-format json`의 토큰 사용량을 비교한다.

- **A (아카이브 참조)**: 증상 + 해당 이슈 카드를 받고 수정
- **B (직접 조사)**: 증상만 받고 스스로 조사해 수정

## 구조

```
cases/NN-name/    에이전트에게 주는 공개본 — TASK.md(증상 보고) · 버그 코드 · 재현 테스트 · test.sh
oracle/NN-name/   비공개 — card.txt(카드 경로·채택 사건) · fix.patch(참조 수정) · hidden/(판정 테스트, hidden.sh)
scripts/verify-cases.sh   케이스 자체 검증
```

실험할 때는 `cases/NN-name`만 임시 폴더로 복사해서 에이전트를 돌린다. `oracle/`은 에이전트가 보면 안 된다.

## 케이스

| # | 언어 | 카드 (`study-note/issue/…`) |
|---|------|------|
| 01 | Java | java/language-semantics-traps |
| 02 | Java | cross-cutting/concurrency/single-writer-ownership |
| 03 | Java | cross-cutting/data/time-semantics |
| 04 | Python | python/language-and-stdlib-traps |
| 05 | Python | cross-cutting/data/pagination-cursor-integrity |
| 06 | Python | cross-cutting/reliability/atomic-file-replace |
| 07 | TS | typescript/js-language-traps |
| 08 | TS | cross-cutting/reliability/debounce-trailing-contract |
| 09 | TS | cross-cutting/data/float-nan-semantics |
| 10 | Shell | shell/exit-status-semantics |

## 케이스 검증

```
bash scripts/verify-cases.sh          # 전체
ONLY=03 bash scripts/verify-cases.sh  # 하나만
```

케이스마다 다음을 확인한다.

- buggy 상태에서 `test.sh`와 `hidden.sh`가 둘 다 실패한다.
- `fix.patch`를 적용하고 hidden을 덮어쓴 뒤에는 둘 다 통과한다.

## 요구 환경

- JDK 21: `$HOME/.sdkman/candidates/java/21.0.5-tem`
- Python 3.12: stdlib만 사용
- Node 18 + 전역 `tsc`
- bash, GNU patch
- Linux: 06 hidden이 `RLIMIT_FSIZE`를 쓴다.
