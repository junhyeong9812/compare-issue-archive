# log — experiment-v3

## 타임라인

| 시각 | 사건 | 결과/결정 |
|------|------|----------|
| 2026-10-09 | 사용자 "ADR-0003 프로젝트 고유 케이스로 진행하자" + 인터뷰 8문항 → ADR-0003·명세 | SPEC=1·MODE=auto 기록 |
| 2026-10-09 | task01: 러너 `--archive-dir`(set_archive), A/F 프롬프트를 템플릿화, CARD_FILE `cia-lib\d*` | 회귀: v2 기본 경로에서 A/F/P/L/B 프롬프트가 변경 전과 동일(stash 비교), /tmp/cia-lib3 지정 시 F 경로 반영 |
| 2026-10-09 | worker-brief-v3.md 작성 → 파일럿 워커 3(Opus) 병렬: 01 Java(fxq 이월 환율 표식), 02 Python(ledgerctl 메모 바이트 상한 보류), 03 TS(mqpub 키 앞 16바이트 dedupe) | 회수 대기 |
| 2026-10-09 | 워커(Python) 회수: 02-expense-close — ledgerctl 메모 UTF-8 48바이트 초과 시 exit 0·영수 번호 주고 st 4 보류(list 제외), 28파일/1,006줄, 미끼 5, 우회 7종 중 대안 정답(40자 자르기 제거)만 hidden 통과·test 실패(보이는 테스트가 '정상 전표 내용 유지'를 고정), strings 규칙어 0, verify 3/3 | 메인 점검: verify PASS, TASK.md에 벤더 동작 언급 없음, 보이는 테스트에 48/49 상수 없음. 대안 정답 차단은 TASK 기대('정상 전표 내용 그대로')와 일치해 수용 |
| 2026-10-09 | 워커(TS) 회수: 03-order-alerts — mqpub 한 배치 안에서 키 앞 16바이트가 같으면 첫 것만 저장(exit 0·accepted 요청 건수), 30파일/1,133줄, 미끼 5, 우회 9종 중 순번 접두만 hidden 통과(규칙상 올바른 구성 — 정답 인정), verify 3/3 | 메인 점검: verify PASS. 수용하되 기록: TASK의 '회차 하나는 한 번에 발행' 문장이 배치 방향을 약하게 가리킴, 난수 키 수정은 hidden 미차단(재발행 멱등성 미검사), 카드 분류 폴더(ledgerctl/ vs messaging/)는 인덱스에서 통일 |
| 2026-10-09 | 워커(Java) 회수: 01-overseas-card-settlement — fxq 휴장일 조회 시 exit 0·전 영업일 값 + '~N' 표식, 정산값은 --fix 재조회(휴장일 --fix 값은 바이너리 밖에서 계산 불가), 30파일/998줄, 미끼 4, 우회 10종 중 대안 정답(정산 경로만 항상 --fix)만 통과 | 메인 점검: v3 전체 verify 3회 3/3, 바이너리 ELF strip, 케이스에 카드·아카이브 용어 0. 위험 기록: --fix가 --help에 보여 B가 빨리 추측할 수 있음 → 파일럿에서 확인 |
