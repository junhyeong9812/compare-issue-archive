# 요구사항 명세서 (requirement-spec)

> 작성일: 2026-10-08 · 작업 폴더: `docs/plans/2026-10-08/case-build/`

---

## 0. 요구사항 원문 (인터뷰 기록)

- 원문: "/home/jun/project/ 여기에 compare-issue-achive? 폴더 하나 만들어서 현재 issue에 기록된 문서를 기반으로 실제 기록된 문제가 일어나는 코드들을 만들어놓고 해당 issue문서를 보고 문제를 해결하는 방안이랑, 에이전트가 자료를 조사해서 직접 해결하는 방식에 대한 토큰이 얼마나 덜 쓰이는지?를 비교해서 실제 issue문서를 아카이브 하는게 얼마나 도움이 되는 지 보고 싶어. 우선 이작업부터 해볼까? 실제 문제가 발생하는 코드들을 만들어보고 이때 예제는 10개 기준으로 만들어서 진행시켜보자."
- Q/A:
  - 범위 → **케이스 10개만**(증상·버그 코드·재현 테스트·판정 테스트). 토큰 비교 실험은 다음 작업.
  - 선정 → Claude 제안 10개 승인(아래 §1 표).
  - 언어 → 카드의 원래 언어(cross-cutting은 Java/TS/Python 중 적합한 것).
  - 측정(차기 실험 전제) → `claude -p --output-format json` 헤드리스 usage.
  - 격리 → 공개본(`cases/`)·비공개(`oracle/`) 분리, 실험 시 cases/NN만 임시 폴더로 복사.
  - 폴더 → `/home/jun/project/compare-issue-archive` + 로컬 git init(원격 없음).
  - 모드 → auto.

## 1. 목표·대상 (필수)

`/home/jun/project/compare-issue-archive`에 study-note `issue/` 카드 10개 각각의 실제 문제를 재현하는 케이스를 만든다 — 각 케이스는 실행하면 문제가 드러나고(재현 테스트 red), 참조 수정을 적용하면 재현+판정 테스트가 green이 된다.

| # | 언어 | 카드 (`study-note/issue/…`) |
|---|------|-------------------|
| 01 | Java | java/language-semantics-traps |
| 02 | Java | cross-cutting/concurrency/single-writer-ownership |
| 03 | Java | cross-cutting/data/time-semantics |
| 04 | Python | python/language-and-stdlib-traps |
| 05 | Python | cross-cutting/data/pagination-cursor-integrity (sqlite) |
| 06 | Python | cross-cutting/reliability/atomic-file-replace |
| 07 | TS | typescript/js-language-traps |
| 08 | TS | cross-cutting/reliability/debounce-trailing-contract |
| 09 | TS | cross-cutting/data/float-nan-semantics |
| 10 | Shell | shell/exit-status-semantics |

구조:
```
cases/NN-<중립 이름>/   TASK.md(사용자 관점 증상 보고 — 원인·카드 용어 없음) · 버그 코드 · 재현 테스트 · test.sh
oracle/NN-<…>/          card.txt(카드 경로·채택한 사건 유형) · fix.patch(참조 수정) · 판정 테스트(hidden)
scripts/verify-cases.sh  전 케이스 자동 검증(아래 §5)
README.md               구조·실험 전제 설명
```

## 2. 경계·불변식 (필수)

- 각 케이스의 문제는 **카드에 기록된 사건 유형 중 하나**를 재현한다(카드에 없는 버그 날조 금지 — card.txt에 카드 내 근거 섹션 명시).
- buggy 상태: 재현 테스트 **실패**(결정적 — 타이밍 의존 테스트는 고정 시계/결정적 인터리빙으로).
- fix.patch 적용 상태: 재현 + 판정 테스트 **전부 통과**.
- `cases/` 안에는 원인·해결 힌트가 없다: 카드 슬러그·카드 핵심 용어·"bug here" 류 주석 0건(누출 스캔).
- 외부 의존성 최소: Java = JDK 21 단일 파일(외부 라이브러리 없음, 자체 assert 러너), Python = stdlib(unittest·sqlite3), TS = Node 18 + 설치된 tsc 또는 `node --test`(npm install 불필요한 형태 우선), Shell = bash.

## 3. 기준소스 (필수)

study-note `issue/` 각 카드의 `1-question.md`·`2-summary.md`·`3-answer.md`(현재 main 작업트리 기준).

## 4. 금지영역 (필수)

- study-note repo(`issue/` 포함) 수정 금지 — 읽기만.
- 원격 저장소 생성·push 없음.
- 케이스 코드에 실제 프로젝트 식별자 사용 금지(카드처럼 추상화 이름).

## 5. 검증 방법 (필수)

- `scripts/verify-cases.sh`: 케이스마다 임시 폴더에 복사 → test.sh **실패 확인** → fix.patch 적용 + 판정 테스트 복사 → test.sh·판정 테스트 **통과 확인**. 10/10 기대 결과 일치가 완료 조건(exit 마스킹 없이).
- 누출 스캔: `cases/`에서 카드 슬러그·핵심 용어 grep 0건.
- 결정성: verify-cases.sh 3회 반복 동일 결과.

## 6. stakes (필수)

- 판정: **낮음** — 새 로컬 샌드박스, 기존 코드·데이터 영향 없음, 되돌리기 쉬움. 실험 타당성(누출·난이도)은 차기 실험 작업에서 재검토.

---

## 7. 자율성

- [x] auto
- [ ] lazy

## 8. load-bearing 가정

1. 각 카드에 "실행으로 결정적으로 재현 가능한" 사건 유형이 최소 1개 있다 — 착수 직후 10개 카드 정독으로 실증, 없으면 대체 후보(ordering-total-order·silent-truncation-marker·in-band-signaling-collision)로 교체 후 보고.
2. 로컬 툴체인(JDK 21 sdkman, Node 18, Python 3.12, bash)으로 외부 설치 없이 실행된다 — 첫 케이스 스모크로 실증.

## 9. task 분해

| task | 목표 | 의존 | acceptance |
|------|------|------|-----------|
| 01 | 골격(README·verify-cases.sh·git init) + 카드 10개 정독, 케이스별 사건 유형 확정 | — | 사건 유형 10개 log 기록 |
| 02 | 케이스 01~10 작성(언어별 워커 병렬 가능) | 01 | 케이스별 red→green |
| 03 | 전체 검증·누출 스캔·3회 반복 + 커밋 | 02 | §5 전부 충족 |

---

## 승인 상태

- [x] 필수 6칸 전부 기입
- [ ] 사용자 합의 → SPEC=1
- [ ] 자율성 선택 → MODE 기록 (auto 응답 받음)
