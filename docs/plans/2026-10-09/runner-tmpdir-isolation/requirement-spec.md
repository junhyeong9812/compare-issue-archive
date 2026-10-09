# 요구사항 명세서 (requirement-spec)

> 작성일: 2026-10-09 · 작업 폴더: `docs/plans/2026-10-09/runner-tmpdir-isolation/`

---

## 0. 요구사항 원문 (인터뷰 기록)

- 원문: "진행하자"(5차-a 보고의 "/tmp 격리 → 5차-b" 순서에 동의)
- Q/A:
  - 격리 범위 = TMPDIR + 감사 기록(권장). bwrap은 AppArmor 제약으로 쓸 수 없고, 거부 규칙은 넣지 않는다.
  - 실행 폴더 밖 /tmp 삭제 실행 = 유효로 두고 표시만(권장).

## 1. 목표·대상 (필수)

`scripts/run_experiment.py` 한 파일만 바꾼다.

1. `sandbox_env`·`run_one`: 실행마다 `run_dir/tmp`를 만들고 `TMPDIR`을 그 경로로 준다. mktemp 결과가 실행 폴더 안에 생기고, 실행 끝의 `rmtree(run_dir)`가 함께 치운다.
2. 감사 보조 필드 `tmp_rm_outside`: 로그의 Bash 명령 중 `rm`이 실행 폴더 밖 `/tmp/...` 경로를 대상으로 하면 그 명령 조각을 목록으로 기록한다. `valid`에는 영향이 없다.
3. 기존 감사가 TMPDIR 이동 때문에 오탐하지 않게 한다. 실행 안 Claude Code의 작업 폴더가 `$TMPDIR/claude-<uid>/...`로 옮겨 가면 `/tmp/claude-\d+` 패턴에 걸릴 수 있으므로, 자기 run_dir 경로는 감사 전에 정규화한다(195행의 기존 정규화와 같은 방식).

## 2. 경계·불변식 (필수)

- 프롬프트·CLI 인자·deny 규칙·권한 모드는 바꾸지 않는다(3~5차와 같은 에이전트 조건).
- 바뀌는 환경 변수는 `TMPDIR` 하나다.
- 기존 감사 패턴은 그대로 둔다. 자기 실행 폴더 경로의 정규화만 더한다.
- `valid` 판정식은 바꾸지 않는다. `tmp_rm_outside`는 표시용 필드다.
- 기존 결과(`results*/`)·케이스·아카이브·oracle은 수정하지 않는다.

## 3. 기준소스 (필수)

- `scripts/run_experiment.py` 현재 코드(178-196행 sandbox_env·audit, 330-396행 run_one)
- v4·v5a 원시 로그의 `/tmp` 삭제 사례 6건(log.md)

## 4. 금지영역 (필수)

- study-note 읽기·수정 금지.
- 기존 케이스·결과·아카이브 수정 금지.
- push·외부 발행 금지(사용자 확인 후만).

## 5. 검증 방법 (필수)

- 단위: `tmp_rm_outside` 판정 함수를 기존 사례에 적용한다.
  - 광역 글롭 2건과 `/tmp/tmp.*/a.txt`는 잡혀야 한다.
  - `/tmp/cia-runs/<자기 run_id>/...` 삭제와 `rm` 없는 `/tmp` 언급은 잡히지 않아야 한다.
- 회귀: 기존 5차 로그 몇 개에 바뀐 audit을 다시 돌려 위반 목록이 이전과 같은지 본다(0건 유지).
- 스모크: 실실행 1회(Haiku, 싼 케이스 1개). 확인할 것:
  - valid=True
  - 에이전트 환경에서 TMPDIR이 run_dir/tmp인지(로그의 mktemp 경로, 또는 `echo $TMPDIR`가 있으면 그 출력)
  - 감사 오탐 없음
  - 스모크 행·폴더는 별도 결과 폴더(`results-smoke-tmpdir`)에 두고, 확인 뒤 지운다.

## 6. stakes (필수)

- 판정: **낮음** — 로컬 실험 러너다. 실험 조건(프롬프트·권한)은 바뀌지 않고, 재실행으로 복구할 수 있다.

---

## 7. 자율성

- [x] auto
- [ ] lazy

## 8. load-bearing 가정

1. Claude Code `-p`의 Bash 도구가 부모 환경의 TMPDIR을 그대로 물려받아 mktemp가 그 경로를 쓴다. 스모크로 바로 확인하고, 아니면 멈추고 보고한다.
2. TMPDIR 이동으로 Claude Code 자기 작업 폴더 경로가 바뀌어도, run_dir 정규화만으로 감사 오탐이 사라진다.

## 9. task 분해

| task | 목표 | acceptance |
|------|------|-----------|
| 01 | TMPDIR 주입 + 감사 정규화 + `tmp_rm_outside` | 단위·회귀 |
| 02 | 스모크 1회 + log·NEXT·5차-a 분석 정정(6건 → 광역 2건) + 커밋 | 스모크 valid·TMPDIR 확인 |
