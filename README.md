# compare-issue-archive

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
