# 요구사항 명세서 (requirement-spec)

> 작성일: 2026-10-09 · 작업 폴더: `docs/plans/2026-10-09/experiment-v4/`
> 설계 정본: `docs/adr/0004-stale-cards-and-archive-scale.md`.

---

## 0. 요구사항 원문 (인터뷰 기록)

- 원문: "2번과 3번까지만 해보자" (NEXT의 4차 후보 + 러너 개선)
- Q/A:
  - 틀린 카드 = 낡은 카드(벤더 버전 변경, 3차 바이너리 유지).
  - arm = L′·P′·F′.
  - 규모 = 22 → 100 → 300.
  - 모델 = Sonnet·Haiku × 3회.
  - 낡음 단서 = 버전 표기 있음.
  - 케이스 = 10케이스 전부.
  - 러너 = hidden stderr 수집, 실패 실행 폴더 보존, 카드 열람 판정 개선(3개 전부).
  - 자율성 = auto(이전 합의 승계).

## 1. 목표·대상 (필수)

1. 러너 개선(`scripts/run_experiment.py`, 필요 시 `summarize.py`):
   - hidden 실패 기록에 stderr를 포함한다.
   - 실패·변조 실행의 작업 폴더를 결과 폴더(`<results>/failed/<run_id>/`)에 보존한다.
   - 아카이브에서 실제 읽은 카드 경로 목록과 정답 카드 열람 여부·바이트를 행에 기록한다.
   - `--summary-file`(기본 `summary.txt`)을 추가한다.
2. 4차 실험(ADR-0004):
   - `cases-v4/`·`oracle-v4/`: 3차 사본 + `--version` 지원 바이너리 + `summary-stale.txt`·`stale-fix.patch`.
   - `archive-v4-stale/`: 관련 카드 10장을 낡은 판으로 교체한 아카이브.
   - `archive-v4-100/`·`archive-v4-300/`: 미끼 카드를 늘린 아카이브.
   - 실행: 낡은 카드 180회 + 규모 120회 + 기준선 확인 10회. 결과는 `results-v4-*/`.

## 2. 경계·불변식 (필수)

- 러너 변경은 기존 실행과 호환된다. 기본값에서 arm 프롬프트·판정·기존 행 필드 의미가 같고, 새 필드는 추가만 한다.
- cases-v4는 cases-v3와 `--version` 외 동작이 같다. verify 3회 PASS, `strings` 규칙어 0.
- 낡은 카드 규칙을 그대로 따른 수정(`stale-fix.patch`)은 hidden에서 실패한다. 낡은 카드도 버전 표기 외에 정답 규칙을 누설하지 않는다.
- 규모 아카이브의 미끼 카드는 정답 도구명과 충돌하지 않는다. 3차 22장은 그대로 포함한다.
- arm 차이는 프롬프트 추가분뿐이다. 그 밖의 조건(격리 HOME·$2·deny+감사·판정·동시 ≤5)은 3차와 같다.
- 기존 케이스·결과·아카이브(`cases*/`·`oracle*/`·`archive-v3/`·`results*/`)는 수정하지 않는다.

## 3. 기준소스 (필수)

ADR-0004. 케이스·카드 요건은 ADR-0003과 `docs/plans/2026-10-09/experiment-v3/worker-brief-v3.md`를 승계한다.

## 4. 금지영역 (필수)

- study-note 수정 금지(형식 참고는 `/tmp/cia-lib` 사본).
- 기존 케이스·결과·아카이브 수정 금지.
- 원격 push·발행 없음.

## 5. 검증 방법 (필수)

- 러너: 3차 기본 경로 회귀(L·P·F·B 프롬프트 동일), 실패를 일부러 만든 실행 1회로 stderr 기록·폴더 보존·카드 열람 필드 확인, 3차 로그 몇 개로 새 parse 필드 재계산.
- cases-v4: verify 3회 PASS, `--version` 출력, `strings` 검사, `stale-fix.patch` 적용 시 hidden 실패.
- 규모 아카이브: 카드 수·인덱스 행 수·도구명 충돌 0.
- 실험: 행 유효(무효는 재실행·보존), 손 검산 1회, 3차 대비 표.

## 6. stakes (필수)

- 판정: **낮음** — 로컬 실험, 재실행으로 복구 가능.

---

## 7. 자율성

- [x] auto
- [ ] lazy

## 8. load-bearing 가정

1. 케이스마다 "이전 버전 규칙"을 그럴듯하게 정할 수 있고, 그 규칙을 따른 수정이 hidden에서 실패한다 — 케이스별 `stale-fix.patch`로 실증한다.
2. 바이너리에 `--version`만 추가해도 B의 탐색이 3차와 크게 다르지 않다 — Sonnet B 10회로 확인한다.

## 9. task 분해

| task | 목표 | acceptance |
|------|------|-----------|
| 01 | 러너 개선 4건 | 회귀 + 실패 실행 1회 확인 |
| 02 | cases-v4·oracle-v4(바이너리 재빌드) + 낡은 카드 10·요약 10·stale-fix 10 | verify 3회, stale-fix hidden 실패 |
| 03 | 규모 미끼 카드 278장 + 인덱스 2개 | 카드 수·충돌 검사 |
| 04 | 기준선 확인 B 10회 → 낡은 카드 180회 | 유효 행 |
| 05 | 규모 120회 + 분석·ADR append·커밋 | 손 검산 |
