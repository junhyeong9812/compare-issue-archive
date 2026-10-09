# log — runner-tmpdir-isolation

## 타임라인

| 시각 | 사건 | 결과/결정 |
|------|------|----------|
| 2026-10-09 | 사용자 "진행하자"(/tmp 격리 → 5차-b 순서 동의, push는 별도 미응답) | 작업 폴더 생성, 인터뷰 |
| 2026-10-09 | 사전 조사: `sandbox_env`는 HOME만 바꾸고 TMPDIR은 호스트 상속(scripts/run_experiment.py:178-182). v4·v5a 로그 중 `/tmp` 삭제 명령 6건: 광역 글롭 `rm -rf /tmp/tmp.*/` 2건(v4-stale-haiku 08-L r2, v5a-g1-stale-haiku 08-L r1), 좁은 글롭 `/tmp/tmp.*/a.txt` 1건, 자기 mktemp 폴더 이름 지정 삭제 3건 | v5a analysis의 "6건" 서술을 정정할 것 |
| 2026-10-09 | 사용자 명세 합의(이대로, auto) → SPEC=1·MODE=auto | |
| 2026-10-09 | task01 구현: sandbox_env에 TMPDIR=run_dir/tmp, audit에 자기 TMPDIR 정규화, `tmp_rm_outside` 필드 | 회귀: v4-stale·v5a 900 로그에 바뀐 audit 재적용 → 기존 audit_violations와 차이 0. 합성 입력: `rm -rf /tmp/tmp.*/`·`rm /tmp/z.tsv` 잡힘, 자기 run_dir/tmp 삭제·`echo /tmp/foo` 안 잡힘, `cd /tmp && rm -rf x`(상대 경로)는 못 잡음(한계) |
| 2026-10-09 | 관찰: 과거 900 로그에 tmp_rm_outside 적용 시 71회 해당. 대부분 에이전트가 고정 이름으로 만든 자기 탐침 파일(`/tmp/idchk-probe`·`/tmp/pgx` 등) 정리. 광역 글롭(`/tmp/tmp.*/`, `/tmp/idchk-*`)도 있어, 동시 실행 중인 같은 케이스 다른 arm의 고정 이름 파일을 지우거나 읽을 수 있는 경로가 있었다 | TMPDIR는 mktemp 사용분만 막는다. 고정 이름 `/tmp/...` 직접 사용은 이번 범위 밖 — NEXT 등재 |
| 2026-10-09 | 스모크: Haiku 04 L r1(cases-v4·oracle-v5·summary-fresh) valid·success, audit 0, tmp_rm_outside []. 로그에 mktemp 경로가 안 찍혀 별도 확인: 같은 sandbox_env로 claude -p haiku에 `echo $TMPDIR; mktemp` → `TMPDIR=/tmp/cia-runs/99-X-r0-envcheck/tmp`, mktemp 결과 그 안, Claude Code 자기 폴더도 `$TMPDIR/claude-1000`로 이동, audit 0 | 가정 1·2 실증. 스모크 결과 폴더·확인 폴더 삭제 |
| 2026-10-09 | 커밋(코드만) | |

## 생략한 검증

없음. stakes 낮음이라 리뷰는 셀프 diff 검토로 했다. 프롬프트·인자·deny 규칙에 변경이 없음을 diff로 확인했다.

## 완료 요약

- `scripts/run_experiment.py`:
  - `sandbox_env(sb_home)` → `sandbox_env(sb_home, sb_tmp)`. `TMPDIR`을 추가했다.
  - `run_one`이 `run_dir/tmp`를 만든다.
  - audit이 `/tmp/cia-runs/<run_id>/tmp/`를 `<own-tmp>/`로 정규화한다.
  - 새 함수 `tmp_rm_outside`와 행 필드 `tmp_rm_outside`를 추가했다(valid에는 영향 없음).
- 한계: 에이전트가 고정 이름 `/tmp/<이름>`을 직접 쓰고 지우는 것은 막지 못한다. 동시 실행 간 오염 경로가 남는다(NEXT).
