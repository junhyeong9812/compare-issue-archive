# log — experiment-v2

## 타임라인

| 시각 | 사건 | 결과/결정 |
|------|------|----------|
| 2026-10-08 | 사용자 "진행하자" — ADR-0002(개정) 합의 → 명세 작성 | SPEC=1·MODE=auto 기록 |
| 2026-10-08 | task01: verify-cases.sh CASES_DIR/ORACLE_DIR, 러너 --cases-dir 등·arm P(A′, card.txt 경로 존재 확인)·지표 first_edit_turn·archive_result_bytes | 1차 회귀 verify 10/10, card_path 10개 존재, parse_log 스모크 |
| 2026-10-08 | task02: v2 브리핑(worker-brief-v2.md) → 언어별 워커 4개(Opus) 병렬 | 회수 대기 |
| 2026-10-08 | 워커(Shell) 회수: 10-partner-settlement — 카드 질문5/변형E(iconv 부분 변환의 비0을 실패로 오인해 원문 폴백), 30파일/804줄, 미끼 5, 우회 6종 중 끝단 땜질·부분 수정 hidden에서 차단, verify 3/3 | 워커가 본 scripts/ M 상태는 메인 task01 변경(정상). iconv glibc 2.39 의존 기록 |
| 2026-10-08 | 워커(Python) 회수: 04-library-catalog(변형A, 반복 키→list 필드에 문자열 슬라이스, 29파일/761줄), 05-order-sync(정답3/변형B, top만 검사하는 오프셋 커서, 27/658), 06-store-catalog(정답3, 고정 이름 백업 디렉터리 재사용, 26/749) — verify 3/3, 땜질·미끼 우회 hidden 차단 | 워커 보고의 기준 SHA 표기(db07feb8·main)는 부정확 — 실제 feat/case-build 9686c2a 위 미커밋 |
| 2026-10-08 | 워커(TS) 회수: 07-order-board(정답3/변형C, 도메인 맵 스프레드 병합의 키 덮어쓰기 — tsc 7은 계산 enum 키 중복도 TS1117로 거부해 스프레드로 숨김, 30/823), 08-job-console(정답5·7/변형B, 보류 emit 역순·유실 + 수신 종결 우선, 원인 2곳, 27/798), 09-device-sync(변형A, deepEqual의 NaN 반사성, 26/700) — verify 3/3 | 09는 NaN 데이터가 불가피(1차와 동일) |
| 2026-10-08 | 워커(Java) 회수: 01-account-security(질문2, static 초기화 순서로 원시 int 0 보관, 30/1160), 02-menu-board(질문6/방안7, ORM 전체 컬럼 UPDATE lost update), 03-field-notes(질문5/변형C, 저장 계층 감사 스탬프가 클라이언트 시각을 서버 시각으로 덮음) | 메인 점검: 02·03 TASK.md 부재 발견(워커 보고와 불일치) → 워커 재지시로 작성, 누출 0·verify PASS. 02·03 파일 31개(상한 30 초과 1) — 규모 요건 취지(한 번에 다 못 읽음) 충족으로 허용 판정 |
| 2026-10-08 | 메인 점검: 10케이스 buggy 실패가 모두 E2E 단언 실패(컴파일·환경 오류 아님), 규모 26~31파일/700~1,211줄, 판정 PROTECTED를 tests?/·fixtures/ 로 확장(v2 테스트 폴더 구조) | |
| 2026-10-08 | v2 verify 3회 연속 10/10, 전역 누출 0 → 코드 커밋 | 파일럿 착수(B×1, results-v2-pilot) |
| 2026-10-08 | 파일럿 B×10: 전부 성공. 게이트(도구≥8 또는 토큰≥180k) 통과 1/10(10번 223k). 나머지 도구 4~7·토큰 116k~179k. v1 B 중앙값 88k 대비 약 1.3~2.5배로 난이도는 올랐으나 기준 미달 | B는 `cat` 한 번에 파일 4개씩 읽는 식으로 1,000줄 규모를 1~2회 호출로 소화 — 규모 확대로는 탐색을 강제하지 못함. 명세상 재설계 1회 경로 vs 진행은 사용자 결정 |
| 2026-10-08 | 지표 결함 발견: first_edit_turn이 assistant 이벤트(블록 단위) 기준이라 num_turns보다 커짐 → message id 단위로 수정 | 파일럿 로그 재파싱으로 확인 |
| 2026-10-08 | 사용자 결정: 게이트 미달 기록 후 그대로 본 실험(B/F/P × 3, Sonnet) | 명세 §5 게이트 조항의 예외 — 사용자 합의 |
| 2026-10-08 | 본 실험 90회 완료(exit 0): 유효 90·무효 0. 성공 B 28·F 28·P 30. 실패 4건 전부 test 통과·hidden 실패(05 B×2·F×1, 06 F×1) | 실행 폴더는 실행 후 삭제돼 실패 상세 재판정 불가 — hidden 출력이 stderr라 hidden_tail 공란(러너 한계, 기록) |
| 2026-10-08 | summarize: arm P·결과 디렉터리 인자 지원. 1차 summary 재생성(헤더 문구만 변경, 수치 동일) | |
| 2026-10-08 | 손 검산: 변동 토큰 중앙값 B 42,371·P 71,330·F 88,870 별도 스크립트 계산. 지표 결함: card_tool_calls가 `cd 폴더 && cat 1-question.md` 형태 미검출 → archive_result_bytes로 대체 확인(P 30/30 >2KB) | analysis에 한계 명시 |

## 완료 요약
- 케이스 v2 10개(26~31파일·700~1,211줄, 원인/증상 분리·미끼·E2E)와 2차 실험 90회. 결론: 아카이브 우선 정책은 토큰 ×1.87, 카드 직접 제공도 ×1.35. 다만 낯선 문제(10)에서 P가 토큰 −35%, 어려운 문제(05)에서 정답률 1/3→3/3 — 카드 가치는 조건부로 실재.
- 이월: 러너 hidden_tail이 stderr 미수집, card_tool_calls 과소 집계, 실패 실행 폴더 미보존.
