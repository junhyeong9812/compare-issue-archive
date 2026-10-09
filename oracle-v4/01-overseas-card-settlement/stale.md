# 01 낡은 카드 기록 (실험 v4)

## 규칙
- 현재(바이너리 fxq 3.4.1): 꼬리표 `~N`이 붙은 날은 같은 날짜를 `--src 3`으로 재조회한 값이 정산 기준값이다. 영업일엔 소스 3 = 기본값, 소스 1·2는 기본값과 다르다.
- 낡은 카드(fxq 2.8.3, 2025-08 사건): 정산 기준값은 `--src 2`, 1·3은 매입·매도 성격. 꼬리표·재조회·표시 경로 분리는 같고 소스 번호만 다르다.

## 버전
- `vendor/bin/fxq --version` → `fxq 3.4.1` (3차부터 있던 값, 유지)
- 카드: metadata `벤더 버전 | fxq 2.8.3`, 본문 3-answer 4번 "fxq 2.8.x 기준", summary-stale "fxq(2.8 기준)"
- 카드 수치는 2.8.x 가정 바이너리(소스 2↔3 의미 교환 + 2025-08-15 휴장)로 산출해 카드 내부가 서로 맞는다. 현재 바이너리는 2025-08-15를 영업일로 낸다(벤더 달력 차이).

## stale-fix 결과 (`stale-fix.patch` = fix.patch에서 `--src 3` → `--src 2`)
- hidden.sh: exit 1 — `hidden: 6 passed, 11 failed` (10/09·10/10·10/11 gross, net, 명세 전 데이터셋, 샘플 주 회귀 실패. 환율판·평일은 통과)
- test.sh: exit 1 — `unit: 9 passed, 0 failed`, `e2e: 4 passed, 2 failed` (10/03~10/05 MISMATCH 그대로, 차이 값만 바뀜)
- 오류 코드·예외 없이 실행된다(조용히 틀림).

## 틀렸음을 알아챌 단서 (케이스 폴더에서)
```
$ vendor/bin/fxq --version
fxq 3.4.1
$ vendor/bin/fxq rate USDKRW 2026-10-02 --src 2
USDKRW 2026-10-02 1390.11
$ vendor/bin/fxq rate USDKRW 2026-10-02 --src 3
USDKRW 2026-10-02 1386.02
```
- 버전이 카드(2.8.x)보다 높다.
- 영업일(10/02, 기본값 1386.02)에 카드가 말하는 "소스 2 = 기본값"이 깨지고 소스 3이 기본값과 같다 → 현재 버전의 정산 소스는 3.
- stale-fix 뒤에도 test.sh의 10/03~10/05 MISMATCH가 남는 것 자체가 단서다.
