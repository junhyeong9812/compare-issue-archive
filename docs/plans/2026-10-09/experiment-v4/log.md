# log — experiment-v4

## 타임라인

| 시각 | 사건 | 결과/결정 |
|------|------|----------|
| 2026-10-09 | 사용자 "2번과 3번까지만 해보자"(4차 후보 + 러너 개선) + 인터뷰 7문항 → ADR-0004·명세 | 사용자 명세 합의(이대로 진행) → SPEC=1·MODE=auto |
| 2026-10-09 | task01 러너: hidden_stderr_tail · 실패 실행 작업 폴더 보존(`<results>/failed/<run_id>`, node_modules·target 등 제외) · card_dirs_read/target_card/target_card_read/card_result_bytes(입력 경로 + 도구 결과 본문 서명 줄 대조) · `--summary-file` | 회귀: v2·v3 A/F/P/L/B 프롬프트 변경 전과 동일(stash cmp). 3차 로그 재계산: 기존 필드 일치, 정답 카드 열람 P 60/60·F 60/60(분석의 60/60과 일치), B·L 0 — 경로만으로는 F 50/60(`cd` 뒤 상대 경로 누락)이라 본문 서명 대조를 추가 |
| 2026-10-09 | 러너 실전 확인: Haiku F 09 1회(results-v4-runnertest, 성공, target_card_read=True) · 빈 에이전트 명령으로 실패 경로(kept_work 176K, hidden_tail 기록 후 시험 행·폴더 삭제) · 깨뜨린 Python 케이스 judge로 hidden_stderr_tail 기록 확인 | 통과 |
| 2026-10-09 | task02 착수: cases-v3→cases-v4, oracle-v3→oracle-v4, archive-v3→archive-v4-stale 복사(cp -a). worker-brief-v4-stale.md 작성 → 워커 5(Opus) 병렬, 케이스 2개씩(01·02 / 03·04 / 05·06 / 07·08 / 09·10) | 회수 대기 |
| 2026-10-09 | task03 준비: archive-v3→archive-v4-300 복사, worker-brief-v4-decoy.md 작성(미끼 278장, 워커별 영역·tsv 인덱스 줄) | 낡은 카드 워커 회수 후 착수(동시 ≤5) |
| 2026-10-09 | 워커(07·08) 회수: 07 pushq 현재 5분 경계/카드 10분 경계(pushq 2.3.x, 현재 2.8.0 — 기존 --version 유지, 바이너리 v3와 바이트 동일), 08 stockpush 현재 QTY 0 무시/카드 2 미만 무시(1.9.x, 현재 2.3.0 — --version 추가). 둘 다 verify 3/3, stale-fix hidden 실패·test.sh 통과(조용히 틀림), 탐침 v3/v4 diff는 --help 한 줄뿐, strings·누출 0 | 메인 점검은 전체 회수 후 일괄 |
| 2026-10-09 | 워커(01·02) 회수: 01 fxq 현재 --src 3/카드 --src 2(fxq 2.8.3, 현재 3.4.1 — 바이너리 v3와 동일, 카드 사건을 2025-08 광복절로 옮김), 02 ledgerctl 현재 48바이트/카드 64바이트(4.6.2, 현재 5.2.0 — --version 추가). verify 3/3, stale-fix hidden 실패(01·02는 test.sh도 실패 — 보이는 테스트가 낡은 교정을 잡음), 탐침 diff 0, strings·누출 0 | 기록: 01·02는 낡은 카드를 따르면 test.sh가 먼저 실패해 에이전트가 스스로 알아챌 수 있는 유형, 07·08은 test.sh 통과(완전히 조용함) — 분석 때 두 유형으로 나눠 본다 |
| 2026-10-09 | 워커(05·06) 회수: 05 idchk 현재 앞 12줄/카드 앞 20줄(2.4.x, 현재 3.2.0), 06 pgsim 현재 (TID,금액,AUX)/카드 (TID,금액)·AUX 무관(3.3.x, 현재 4.1.0). verify 3/3, stale-fix hidden·test.sh 모두 실패(보이는 테스트로 알아챌 수 있는 유형), 탐침 diff는 usage 한 줄 | 메인 결정: ①06 stale-fix가 3차 fix와 다른 위치(배치 분리)인 것 수용 — AUX를 줄마다 다르게 채우는 수정은 어떤 낡은 조건으로도 현재 규칙을 통과하므로 불가피 ②05 낡은 판 경로 batch-head-twelve → batch-head-cutoff로 변경("twelve"가 현재 상한 12를 암시 — 낡은 카드와 모순되는 단서). archive-v4-stale 인덱스 1행·카드 첫 줄·oracle-v4/05 card.txt 첫 줄 갱신. 기록: 3차 F·P에서도 이 경로명이 상한을 암시하고 있었다(3차 05 결과 해석의 한계) |
| 2026-10-09 | 워커(09·10) 회수: 09 cfgsvc 현재 "mark 시점 기존 키는 다음 mark까지 옛 값"/카드 1.8.x "mark 뒤 재변경만 보류 — set 앞에서 mark"(현재 2.3.0, 수치 변형 불가라 조건 변형), 10 ptsctl 현재 "숫자 번호 길이 무관 앞 0 제거"/카드 3.6.x "숫자 6자리만"(현재 4.2.0). verify 3/3, stale-fix hidden 실패(09 test.sh도 실패, 10 test.sh 통과), 탐침 diff는 --help 한 줄, strings·누출 0 | 워커가 지적한 인덱스 변경(idchk 경로)은 메인 결정분 |
| 2026-10-09 | 워커(03·04) 회수: 03 mqpub 현재 앞 16바이트/카드 2.6.2 "끝 12바이트, 해시를 키 끝에"(폭만 바꾸면 3차 해시-앞 교정이 현재 규칙도 통과해 위치를 바꿈), 04 xferq 현재 `_` 4개 이상/카드 2.4.3 "마지막 `_` 필드 2자 이하, 0 채움"(개수만 바꾸면 성립 불가). verify 3/3, stale-fix hidden·test.sh 실패 | 메인 결정: 03 낡은 판 경로 mqpub-batch-key-head → mqpub-batch-key-dedupe("head"가 낡은 규칙 "끝"과 모순되는 단서 — 05와 같은 처리) |
| 2026-10-09 | 메인 점검(낡은 카드 전체): v4 verify 3회 연속 10/10 · stale-fix 10/10 hidden 실패(test.sh는 07·08·10 통과=완전히 조용, 나머지 7은 test.sh도 실패) · --version 10/10 · cases-v3↔v4 차이는 vendor/bin뿐 · archive-v3↔v4-stale 차이는 관련 카드 10장 + 인덱스 2행(경로 변경 2) · 미끼 12장 무변경 · summary-stale 10개에 카드 버전 표기·현재 버전 번호 0 · card.txt의 복사 경로 설명 /tmp/cia-lib40으로 정정 | 통과 → 기준선 B·낡은 카드 실행 착수 |
| 2026-10-09 | 워커 w1(결제 미끼) 회수: 56장(100 표시 16·증상 비슷 28), 도구 17개, 폴더당 2,541~4,738B, 금지 규칙어 grep 0, 도구명 충돌 0 | 공유 scratchpad 덮어쓰기(gen.py) 보고 → 실행 중 워커 4에 하위 폴더 사용 알림 |
| 2026-10-09 | 워커 w2(메시징 미끼) 회수: 56장(100 표시 16·증상 비슷 30), 도구 16개, 2,750~5,101B, 금지어 0, 충돌 0. metadata에 '발생' 행 추가(3차 형식과 1행 차이 — 수용) | |
| 2026-10-09 | 워커 w3(인프라 미끼) 회수: 56장(100 표시 16·증상 비슷 26), 도구 19개, 2,916~4,915B, 금지어 0, 충돌 0 | |
| 2026-10-09 | 워커 w5(같은 도구 20 + 커머스·데이터 34) 회수: 54장(100 표시 15·증상 비슷 30), 같은 도구 카드는 운영 주변 사건만(입력 처리 규칙·지어낸 옵션·버전 숫자 없음), 3,286~5,107B, 금지어 0, 충돌 회피 2건(srchidx→prodsrch 등) | |
| 2026-10-09 | 워커 w4(인증·보안 미끼) 회수: 56장(100 표시 15·증상 비슷 28), 도구 18개, 3,379~5,002B, 금지어 0, 충돌 0 | |
| 2026-10-09 | 메인 조립: tsv 5개 278행(100 표시 78) → archive-v4-300(카드 300, 인덱스 300행·50.8KB), archive-v4-100(archive-v3 + 100 표시 78장 = 100, 인덱스 16.8KB). 경로 중복 0·tsv 없는 폴더 0·폴더당 4파일. 메인 누출 스캔에서 fxq 같은 도구 미끼 1장(monthly-quota-exhausted-dashboard-poll)이 '휴장일 이월값' 사고를 언급 → 두 아카이브에서 일반 표현으로 교체 | 규모 아카이브 완료 |
| 2026-10-09 | 실행 착수: run-stale.sh(기준선 B 10 → 낡은 카드 Sonnet 90 → Haiku 90, 병렬 3, /tmp/cia-lib40) + run-scale.sh(F × 100·300 × Sonnet·Haiku, cases-v3, 병렬 2, /tmp/cia-lib41·43) 동시 — 합계 동시 5 | 원본 토큰 만료 임박으로 갱신 대기 중 |
| 2026-10-09 | 전체 실행 완료(EXIT 0 7개): 기준선 10 · 낡은 카드 Sonnet 90·Haiku 90 · 규모 120. 무효 0 · 러너 오류 0 → 재실행 없음 | summarize 7개 생성 |
| 2026-10-09 | 손 검산: Sonnet 09 L′ 원시 로그 result usage 합 [353,714·284,608·362,573] = 행 값, 중앙값 ÷ 3차 09 B 0.76 = 표. v4 B 중앙값 ÷ 3차 B 0.96(케이스별 0.78~1.34, n=1) → 3차 B를 분모로 사용 | 통과 |
| 2026-10-09 | 실패 44건 확인(보존 폴더·hidden stderr): 40건 test 통과·hidden 실패. Sonnet 03 F′ 0/3은 `test.sh 2>&1 \| tail -8`이 앞쪽 FAIL을 잘라 "통과" 보고 — 보존 폴더 재실행으로 FAIL 확인. Sonnet 실패 30건 `--version` 호출 0 | 러너 개선(실패 폴더 보존·stderr)이 원인 규명에 바로 쓰임 |
| 2026-10-09 | 결과: 낡은 카드 ÷B Sonnet L′1.13·P′1.27·F′1.08(성공 21·22·17/30), Haiku 0.88·1.04·1.14(23·27·26/30). 규모 F ÷B Sonnet 22→100→300 0.53→0.69→0.73, Haiku 0.64→0.72→0.79, 120/120 성공·정답 카드 열람 | analysis·ADR-0004 append. `results*/failed/`(7.9MB 작업 폴더 사본)는 .gitignore — 로컬 보존 |

## 완료 요약
- 4차(ADR-0004): 낡은 카드(벤더 버전 변경, 버전 표기 있음)는 카드가 없는 것보다 나빴다. Sonnet ÷B 1.08~1.27에 성공률 17~22/30이고, 조용한 장애에서는 1~2/9였다. 버전 표기만으로는 막지 못했다(Sonnet 실패 중 `--version` 확인 0).
- 규모 300장(경량 인덱스 50.8KB)에서도 F는 정답 카드를 모두 찾았고, ÷B는 0.73·0.79다. 비용은 22장 대비 ×1.38·×1.25로 완만하게 늘었다.
- 코드: 러너 hidden_stderr_tail·실패 폴더 보존·카드 열람 판정(경로 + 본문 서명)·`--summary-file`.
- 데이터: cases-v4·oracle-v4(stale-fix·summary-stale)·archive-v4-stale·archive-v4-100·archive-v4-300.
- 결과: results-v4-baseline·stale(-haiku)·scale100/300(-haiku).
- 사이클 마감: CS 이슈 아카이브 대상 0건(실험 결과뿐). 관찰한 `| tail` exit·출력 마스킹은 하네스 core §4 "그린 위장 점검"에 이미 있는 유형이다.
