# log — case-build

## 타임라인

| 시각 | 사건 | 결과/결정 |
|------|------|----------|
| 2026-10-08 | 명세 합의, SPEC=1·MODE=auto | 착수 |
| 2026-10-08 | 골격: git init, scripts/verify-cases.sh(buggy red·fixed green·hidden 양방향 판정) | 케이스 규약 확정 후 언어별 워커 4개 병렬 위임(Opus) |
| 2026-10-08 | 워커(Shell) 회수: 10-nightly-backup — 카드 질문2/변형B(-e 없는 순차 스크립트), verify 3/3 PASS 보고 | 메인 교차 확인은 전체 검증 때 |
| 2026-10-08 | 워커(Python) 회수: 04-name-romanizer(정답1/변형A, 빈 문자열 in 검사), 05-order-export(정답5/변형C, 필터 후 개수로 종료 판정), 06-inventory-save(정답1/변형A, open('w') truncate) — 각 verify 3/3 PASS, 우회 수정이 hidden에 걸리는 것도 확인 보고 | 06 hidden은 Linux RLIMIT_FSIZE 의존(기록). 메인 교차 확인은 전체 검증 때 |
| 2026-10-08 | 워커(TS) 회수: 07-order-submit(정답4/변형D, await 없는 return), 08-draft-autosave(정답2·3/변형A, 변화 없는 틱 early-return 뒤 flush), 09-config-export(정답3/변형B, === Number.NaN) — 각 verify 3/3 PASS | 08 'dirty' 필드명은 일반 명사·카드 원문에도 있는 관용 이름 → 유지 판정(오탐). tsc 7 + types:[] + 최소 declare |
| 2026-10-08 | 워커(Java) 회수: 01-login-lockout(질문3/변형B, 인자 평가 순서), 02-shipment-labels(질문2/변형A, load-보존 후 전체 save lost update, latch 결정적), 03-daily-sales(질문6/변형D, 양끝 포함 구간 + 다음날 00:00) — 각 verify 3/3 PASS | 02 TASK.md의 스레드 환경 서술은 현실적 증상 보고 범위·두 arm 동일 조건이라 유지 |
| 2026-10-08 | 메인 전체 검증: verify-cases.sh 3회 연속 pass=10 fail=0, exit=0 | 결정성 확인 |
| 2026-10-08 | 메인 buggy 실패 사유 점검: 10건 모두 컴파일/환경 오류가 아닌 단언 실패(증상 메시지) | 그린 위장 없음 |
| 2026-10-08 | 메인 누출 스캔(카드 슬러그·study-note·oracle 경로 전 케이스 grep): 02 테스트의 java.util.concurrent.atomic import 2파일만 | 오탐(Java 표준 패키지명) |
| 2026-10-08 | study-note git status 점검: cs/·scripts/notes·docs/plans/2026-10-08/rules-checker 변경 + 브랜치 docs/rules-checker | 이 작업 산출 아님(타 세션 rules-checker, 12:24~ 생성) — 무접촉, 사용자 보고. issue/ 변경 0 |
| 2026-10-08 | 커밋: feat/case-build 브랜치에 코드 커밋(README·cases·oracle·scripts) | main 직접 작업 회피, 원격 없음 |

## 완료 요약

- 케이스 10개(Java 3·Python 3·TS 3·Shell 1) — 각 케이스는 TASK.md 증상 보고 + 버그 코드 + 재현 테스트, oracle에 카드 매핑·fix.patch·hidden 판정 테스트.
- 워커가 확인한 우회 수정(visible만 통과하는 땜질)이 hidden에서 걸리는 케이스: 02·03·04·05·06·09·10.
- 차기: 토큰 비교 실험(A 카드 제공 / B 직접 조사) — `claude -p --output-format json`, 케이스를 임시 폴더로 복사해 격리. 설계 시 결정할 것: B arm의 study-note 접근 차단 방법, 전역 CLAUDE.md 영향 통제(--bare 류), 반복 횟수, A의 변형(카드 직접 제공 vs issue/ 인덱스에서 검색).
- 아카이브: 이번 작업에서 발생한 CS 이슈 — tsc가 `x == NaN` 리터럴 비교를 TS2845로 거부(케이스를 Number.NaN 비교로 우회)는 기존 카드 float-nan-semantics의 탐지 방법 범주 — 별도 카드 가치 낮음, 아카이브 0건.
