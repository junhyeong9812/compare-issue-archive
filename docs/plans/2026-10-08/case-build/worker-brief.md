# 케이스 작성 워커 공통 브리핑

## 목표
study-note `issue/` 카드에 기록된 실제 문제를 재현하는 "버그 케이스"를 만든다. 차기 실험에서 에이전트 A(카드를 받음)와 에이전트 B(카드 없이 스스로 조사)가 이 케이스를 고치게 하고 `claude -p` 토큰 사용량을 비교한다.

## 배경·제약
- 루트: `/home/jun/project/compare-issue-archive` (git repo, 커밋은 하지 마라 — 메인이 한다)
- 카드: `/home/jun/project/study-note/issue/<카드경로>/` 의 1-question.md, 2-summary.md, 3-answer.md를 **끝까지 읽고**, 카드에 실제로 적힌 사건 유형 중 하나(카드에 없는 버그 날조 금지)를 고른다. 실행으로 결정적으로 재현되고, 증상만 보고서는 원인이 바로 보이지 않는 쪽을 우선한다.
- study-note는 읽기만 한다 (수정 금지).
- 외부 의존성 금지: Java = JDK 21 (`$HOME/.sdkman/candidates/java/21.0.5-tem/bin/javac`·`java`, 외부 라이브러리·JUnit 없음 — 작은 자체 assert/main 러너, 여러 파일이면 javac로 임시 out 디렉토리에 컴파일). Python = 3.12 stdlib(unittest, sqlite3). TS = Node 18 + 전역 `tsc`(7.0.2) 로 컴파일 후 `node --test` 또는 자체 러너(npm install 금지, @types/node 없을 수 있으니 확인 후 필요하면 최소 declare로 대체). Shell = bash.
- 테스트는 결정적이어야 한다: 시간·타이머는 주입 가능한 가짜 시계, 동시성은 결정적 인터리빙(배리어/latch로 강제)이나 충분한 반복으로 100% 재현되는 형태. 3회 연속 같은 결과.
- 실제 프로젝트 식별자 금지 — 카드처럼 일반 이름(OrderService, inventory 등 평범한 도메인)으로.

## 디렉토리 규약 (NN = 배정 번호, name = 원인을 드러내지 않는 중립 이름, 예: `02-stock-counter`)
```
cases/NN-name/
  TASK.md        사용자/운영자 관점 증상 보고서. "무엇을 기대했는데 무엇이 일어났나 + 재현 방법(bash test.sh)". 
                 원인·해결 방향·카드 용어·카드 슬러그 단어 금지. 마지막 줄: "test.sh가 통과하도록 고쳐 주세요. 테스트 파일은 수정하지 마세요."
  test.sh        보이는 재현 테스트 실행. 실패 시 exit≠0, 통과 시 exit 0. 케이스 폴더 안에서 `bash test.sh`로 실행됨(cwd = 케이스 폴더).
  <소스·테스트>   버그 코드는 자연스러워야 한다 — "BUG", "FIXME", 원인 암시 주석 금지. 10~150줄 수준의 작은 앱 코드.
oracle/NN-name/
  card.txt       카드 경로 + 채택한 사건 유형(카드 내 어느 섹션/문장인지 인용 1~3줄) + 근본 원인 1줄 + 올바른 해결 1줄
  fix.patch      참조 수정. 케이스 폴더 기준 `patch -p1`로 적용되는 unified diff (a/파일, b/파일). 소스만 고치고 테스트는 건드리지 않는다.
  hidden/        수정 후 케이스 폴더에 덮어 복사되는 파일들. 반드시 hidden.sh 포함.
    hidden.sh    판정 테스트 실행(케이스 폴더 cwd). 보이는 테스트만 땜질한 우회(특정 입력 하드코딩 등)를 잡도록 같은 불변식을 **다른 입력·경로**로 검사. buggy 상태에서도 실패해야 한다.
```

## 검증 (반드시 직접 실행하고 결과를 보고)
`ONLY=NN bash /home/jun/project/compare-issue-archive/scripts/verify-cases.sh` → `PASS`. (스크립트는 buggy에서 test.sh·hidden.sh 둘 다 실패, fix 후 둘 다 통과를 요구) 3회 반복 동일 결과.
누출 스캔: `grep -riE '<카드 슬러그 단어들>|<카드 핵심 용어>' cases/NN-*` 0건 (오탐이면 사유).

## 기대 출력 (packet)
케이스별: 번호·폴더명 · 카드 내 채택 사건(인용) · 버그 1줄/수정 1줄 · 실제 읽은 파일 · verify 3회 결과 원문 · 누출 스캔 명령+결과 · 미완료 항목. 
## 하지 말 것
scripts/verify-cases.sh 수정, 다른 번호 케이스 수정, git commit, 네트워크 설치.
