# 4차 낡은 카드 워커 공통 브리핑

## 목표

3차에서 맞는 카드는 토큰을 절반 안팎으로 줄였다. 4차는 **벤더 버전이 바뀌어 카드가 낡은 경우**의 비용을 잰다.
- 바이너리 규칙은 3차와 같다(현재 버전).
- 카드는 **이전 버전의 규칙**을 말한다.

필독 순서:
1. `docs/adr/0004-stale-cards-and-archive-scale.md` (D1·D2)
2. `docs/adr/0003-project-specific-knowledge-experiment.md`
3. 담당 케이스의 3차 자료(읽기만):
   - `cases-v3/NN-*/`
   - `oracle-v3/NN-*/` (card.txt·summary.txt·probe.md·blackbox/·fix.patch·hidden/)
   - 맞는 카드 `archive-v3/issue/<카드경로>/`

실험에서는 Sonnet·Haiku 에이전트가 담당 케이스를 세 조건으로 고친다.
- L′: 낡은 결론 요약 2~3줄을 받는다.
- P′: 낡은 카드 경로를 받는다.
- F′: 낡은 카드가 섞인 아카이브를 먼저 찾는다.

## 만들 것 (워커 1명 = 케이스 2개)

이미 메인이 다음을 복사해 두었다: `cases-v4/` = cases-v3, `oracle-v4/` = oracle-v3, `archive-v4-stale/` = archive-v3. 아래는 **v4 사본만** 수정한다.

1. **바이너리 `--version`**
   - `oracle-v4/NN/blackbox/*.c`에 `--version`을 추가한다. 출력은 한 줄 `<도구> <현재버전>`(예: `xferq 3.1.0`).
   - 이미 `--version`이 있는 바이너리(fxq·mqpub·pushq)는 그 버전을 유지한다.
   - 그 밖의 동작(규칙·오류 코드·출력)은 한 바이트도 바꾸지 않는다.
   - 빌드 스크립트를 v4 경로로 맞춰 `cases-v4/NN/vendor/bin/<도구>`에 strip 빌드한다.
   - `--help`에 `--version` 한 줄 추가는 허용한다.
2. **낡은 규칙 정하기**
   - 같은 메커니즘에서 **매개변수·조건만 다른 이전 버전 규칙**으로 정한다. 예: 상한 48바이트 → 이전 64바이트, 앞 16바이트 → 이전 앞 24바이트, `_` 4개 이상 → 이전 6개 이상.
   - 낡은 규칙을 그대로 믿고 고치면 **조용히 틀려야** 한다. 즉 hidden이 실패해야 한다.
   - 존재하지 않는 명령을 쓰게 해서 즉시 오류가 나는 식은 피한다.
3. **낡은 카드** `archive-v4-stale/issue/<같은 카드경로>/`: 1-question·2-summary·3-answer·metadata를 낡은 규칙 기준으로 다시 쓴다.
   - 사건·서비스·구성은 3차 카드를 유지하고, 규칙 수치·조건·대처만 이전 버전 기준으로 바꾼다.
   - 당시 벤더 버전을 metadata 표(`벤더 버전` 행)와 본문 한 곳(예: "xferq 2.4.x 기준")에 적는다. 현재 버전보다 낮게 적는다.
   - 사건 날짜는 과거(예: 2024년)로 한다.
   - "버전이 바뀌면 달라질 수 있다"는 경고, 현재 규칙, 현재 버전 번호는 쓰지 않는다. 실제 낡은 카드처럼 당시에는 맞았던 단정형으로 쓴다.
   - `archive-v4-stale/issue/README.md`(인덱스)는 바꾸지 않는다.
4. **`oracle-v4/NN/summary-stale.txt`**
   - 낡은 카드 결론 2~3줄. 3차 `summary.txt`와 같은 문체·길이로 쓰고, 버전 표기("<도구> 2.4 기준" 등)를 한 번 넣는다.
   - 케이스 코드의 파일명·함수명·변수명은 쓰지 않는다.
5. **`oracle-v4/NN/stale-fix.patch`**: 낡은 카드를 그대로 따른 가장 자연스러운 수정(3차 fix.patch와 같은 위치, 수치·조건만 낡은 규칙)이다.
6. **`oracle-v4/NN/card.txt`**
   - 첫 줄은 `issue/<카드경로>`로 그대로 둔다.
   - 둘째 줄 이후의 근거를 v4 기준으로 갱신한다: 낡은 규칙, 현재 규칙, 버전.
7. **`oracle-v4/NN/stale.md`**(실험 기록용)에 다음을 적는다.
   - 현재 규칙과 낡은 규칙
   - 버전
   - stale-fix 결과
   - 낡은 카드를 따른 에이전트가 틀렸음을 알아챌 수 있는 단서(탐침 명령 1~3개와 출력)

## 확인 (직접 실행해 원문을 보고)

- `CASES_DIR=cases-v4 ORACLE_DIR=oracle-v4 ONLY=NN bash scripts/verify-cases.sh`: PASS 3회. 3차 fix.patch가 그대로 통과해야 한다.
- stale-fix: 임시 폴더에 cases-v4/NN을 복사하고 stale-fix.patch를 적용한 뒤 hidden을 덮어써 `bash hidden.sh`를 실행한다. **실패**해야 한다. `bash test.sh` 결과도 기록한다(통과든 실패든).
- `vendor/bin/<도구> --version` 출력 확인.
- `strings` 규칙어 0: 3차 probe.md·card.txt의 검사 단어를 그대로 쓴다.
- 3차 바이너리와 동작 동일:
  - probe.md의 탐침 명령을 v3·v4 바이너리로 각각 실행한다.
  - 출력이 같은지 diff로 확인한다. `--version`·`--help` 줄은 제외한다.
- 누출:
  - 낡은 카드·요약에 현재 규칙 수치가 없어야 한다.
  - 케이스(cases-v4)에 카드 용어가 없어야 한다.

## 보고 (packet)

케이스마다 다음을 보고한다.
- 현재 규칙과 낡은 규칙(한 문장씩), 현재 버전과 카드 버전
- 바꾼 파일 목록
- verify 3회 원문
- stale-fix의 hidden/test 결과 원문
- v3/v4 탐침 diff 결과
- strings 검사
- 누출 스캔
- 실제 읽은 파일
- 미완료 항목

## 하지 말 것

- `scripts/` 수정
- `cases-v3`·`oracle-v3`·`archive-v3`·기존 결과 수정
- 담당 외 케이스 수정
- git commit
- 네트워크 설치
- study-note 읽기·수정
