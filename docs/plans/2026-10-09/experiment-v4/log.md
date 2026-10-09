# log — experiment-v4

## 타임라인

| 시각 | 사건 | 결과/결정 |
|------|------|----------|
| 2026-10-09 | 사용자 "2번과 3번까지만 해보자"(4차 후보 + 러너 개선) + 인터뷰 7문항 → ADR-0004·명세 | 사용자 명세 합의(이대로 진행) → SPEC=1·MODE=auto |
| 2026-10-09 | task01 러너: hidden_stderr_tail · 실패 실행 작업 폴더 보존(`<results>/failed/<run_id>`, node_modules·target 등 제외) · card_dirs_read/target_card/target_card_read/card_result_bytes(입력 경로 + 도구 결과 본문 서명 줄 대조) · `--summary-file` | 회귀: v2·v3 A/F/P/L/B 프롬프트 변경 전과 동일(stash cmp). 3차 로그 재계산: 기존 필드 일치, 정답 카드 열람 P 60/60·F 60/60(분석의 60/60과 일치), B·L 0 — 경로만으로는 F 50/60(`cd` 뒤 상대 경로 누락)이라 본문 서명 대조를 추가 |
| 2026-10-09 | 러너 실전 확인: Haiku F 09 1회(results-v4-runnertest, 성공, target_card_read=True) · 빈 에이전트 명령으로 실패 경로(kept_work 176K, hidden_tail 기록 후 시험 행·폴더 삭제) · 깨뜨린 Python 케이스 judge로 hidden_stderr_tail 기록 확인 | 통과 |
