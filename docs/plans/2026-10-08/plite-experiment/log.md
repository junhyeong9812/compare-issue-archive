# log — plite-experiment

## 타임라인

| 시각 | 사건 | 결과/결정 |
|------|------|----------|
| 2026-10-08 | 사용자 "한번 해보자" + 인터뷰(요약=카드 결론만·B=2차 재사용·Sonnet+Haiku·auto) → 명세 | SPEC=1·MODE=auto 기록 |
| 2026-10-08 | task01: oracle-v2/NN/summary.txt 10개(433~622B) — 카드 3-answer.md 채택 사건 문단에서 일반 결론만 옮김 | 누출 스캔: 케이스 코드와 겹친 단어 대부분 언어 키워드·내장 함수(static·isinstance·NaN 등). 10의 `out`·`$raw`(카드 예시가 케이스 버그 줄과 같은 변수명), 08의 `closed`(이벤트명)는 일반화해 제거 |
| 2026-10-08 | task02: 러너 arm L(PROMPT_SUMMARY, summary.txt 빈 값 예외), 감사 cia-lib 금지를 B·L 공통으로 / summarize arm L | 스모크: 프롬프트 출력 확인, audit('cia-lib' 접근)=위반·정상=무위반 |
| 2026-10-08 | task03: Sonnet 30 + Haiku 30 실행(EXIT 0 둘 다) | 유효 60·무효 0, 성공 Sonnet 30/30·Haiku 30/30, model 필드 확인 |
| 2026-10-08 | summarize.py가 B 없는 결과 디렉터리에서 KeyError → ÷B 비율만 생략하도록 수정. 재생성으로 바뀐 results-v2/summary.md(헤더 문구만)는 금지영역이라 checkout으로 되돌림 | |
| 2026-10-08 | 손 검산: Sonnet 10-L·10-B·06-L 원시 행 중앙값이 표와 일치. 총 토큰 ÷B: Sonnet L 1.12(P 1.35·F 1.87), Haiku L 1.05(P 1.49·F 1.76) | 분석·ADR-0002 append |

## 완료 요약
- 이상적 검색+요약(L)은 카드 오버헤드를 손익분기 수준(+5~12%)으로 줄이지만 토큰 절감은 없다. 이득은 정답률(B 실패 3 → 0)과 낯선 사건(Sonnet 10번 −42%)에서 난다. 일반 CS 카드용 MCP는 절감 수단이 아니라 오답 보험이다.
- 코드: `scripts/run_experiment.py` arm L(PROMPT_SUMMARY), `scripts/summarize.py` arm L·B 부재 허용. 데이터: `oracle-v2/*/summary.txt`.
- 사이클 마감: CS 이슈 아카이브 대상 0건(새 장애 없음 — 실험 결과뿐).
