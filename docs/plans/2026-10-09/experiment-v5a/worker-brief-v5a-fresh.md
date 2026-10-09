# 5차-a 맞는 카드(fresh) 워커 공통 브리핑

## 목표

4차에서 낡은 카드(이전 벤더 버전의 규칙)를 만들었다. 5차-a는 카드에 버전 방어를 얹고 비용을 잰다.
- 낡은 카드에서 방어가 피해를 줄이는가
- 맞는 카드에서 방어가 이득을 깎는가

비교가 깨끗하려면 **맞는 카드와 낡은 카드가 같은 문서에서 규칙과 버전만 달라야 한다.** 그래서 4차 낡은 카드를 출발점으로, 규칙·대처·버전만 **현재 버전 기준**으로 되돌린 맞는 판을 만든다.

필독:
1. `docs/adr/0005-stale-card-defense.md`
2. `docs/plans/2026-10-09/experiment-v5a/requirement-spec.md`
3. 담당 케이스 자료(읽기만):
   - `oracle-v4/NN-*/card.txt`: 현재 규칙·낡은 규칙·버전
   - `oracle-v4/NN-*/stale.md`
   - `oracle-v4/NN-*/summary-stale.txt`
   - `oracle-v3/NN-*/summary.txt`
   - `oracle-v3/NN-*/card.txt`
   - 3차 맞는 카드 `archive-v3/issue/<v3 경로>/`: 현재 규칙의 원래 서술
   - 4차 낡은 카드 `archive-v4-stale/issue/<경로>/`: 출발점

## 만들 것 (워커 1명 = 케이스 2개)

메인이 다음을 미리 복사해 두었다.
- `archive-v5-fresh/` = archive-v4-stale
- `oracle-v5/` = oracle-v4

**v5 사본만** 수정한다.

### 1. 맞는 카드 `archive-v5-fresh/issue/<4차와 같은 경로>/`

- 4개 파일(1-question·2-summary·3-answer·metadata)을 4차 낡은 카드에서 출발해 고친다.
  - 규칙 수치·조건·대처·예시 수치·코드 조각을 현재 규칙(3차 카드·card.txt의 현재 규칙)으로 되돌린다.
  - 벤더 버전은 **현재 버전**으로 바꾼다. 위치는 metadata `벤더 버전` 행과 본문 표기("fxq 3.4.x 기준" 등) 두 곳이며, 낡은 카드와 같은 자리다.
- 사건·서비스·날짜·문장 구조·제목·길이는 낡은 카드와 같게 둔다.
  - 차이 줄은 규칙·버전과 그에 딸린 수치뿐이어야 한다.
  - 사건 날짜(과거)가 현재 버전과 시기상 어색해도 그대로 둔다. 날짜는 통제 변수다.
- 규칙 서술은 3차 카드와 의미가 같아야 한다.
  - 3차 카드에만 있던 추가 설명을 옮겨 올 필요는 없다.
  - 3차에 없던 새 규칙·힌트는 넣지 않는다.
- 낡은 규칙 수치·이전 버전 번호가 남지 않게 한다.
- `README.md`(인덱스)는 바꾸지 않는다.

### 2. `oracle-v5/NN/summary-fresh.txt`

- `summary-stale.txt`와 같은 문장 구조로 쓰되, 규칙·버전만 현재 기준으로 바꾼다(버전 표기 1회).
- 의미는 3차 `summary.txt`와 같아야 한다.
- 케이스 코드의 파일명·함수명·변수명은 쓰지 않는다.

### 3. `oracle-v5/NN/fresh.md` (기록용)

- 낡은 카드 대비 바꾼 줄 목록(파일·줄 요약)
- 3차 카드와 규칙 의미가 같다는 근거 1~3줄

## 확인 (직접 실행해 원문을 보고)

- `diff -r archive-v4-stale/issue/<경로> archive-v5-fresh/issue/<경로>`: 차이 줄이 모두 규칙·버전 관련인지 확인한다.
- 맞는 카드대로 고치면 hidden이 통과하는지 확인한다(카드 내용 검증).
  - 임시 폴더에 `cases-v4/NN`을 복사한다.
  - `oracle-v4/NN/fix.patch`(3차 정답과 같음)를 적용한다.
  - `oracle-v4/NN/hidden/`을 덮어써 `bash hidden.sh`를 실행한다.
  - 카드 대처 절의 수치·옵션이 fix.patch와 맞는지 눈으로 대조해 보고한다.
- 누출 검사:
  - 맞는 카드·요약에 낡은 규칙 수치·이전 버전 번호가 없다(grep 결과 원문).
  - 요약에 케이스 코드명이 없다.

## 보고 (packet)

- 케이스별 현재 규칙·버전 한 줄
- 바꾼 파일 목록
- diff 줄 수
- 확인 명령과 결과 원문
- 실제 읽은 파일
- 미완료 항목

## 하지 말 것

- `scripts/` 수정
- `cases*`·`oracle-v3`·`oracle-v4`·`archive-v3`·`archive-v4*`·결과 수정
- 담당 외 케이스
- 인덱스 README
- git commit
- 네트워크
- study-note 읽기·수정
- 공유 scratchpad에 파일 쓰기(필요하면 `/tmp/claude-1000/.../scratchpad/v5a-wN/` 아래만)
