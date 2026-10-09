# fresh — 07-autopay-reminders (5차-a 맞는 카드 기록)

- 카드: `archive-v5-fresh/issue/pushq/schedule-offslot-silent-ticket/`
- 현재 규칙·버전: `pushq 2.8.x` — `schedule --at` 이 5분 경계(분 5의 배수·초 00, 300초)가 아니면 티켓·exit 0 이지만 큐 미적재. 교정 = 직전 5분 경계로 내림.
- 만든 방법: 3차 카드(archive-v3) 4파일을 그대로 두고, 4차 낡은 카드에서 규칙이 아닌 줄(사건 날짜 2024-08·벤더 버전 행·"규칙 (… 기준)" 머리)만 가져와 버전을 2.8.x로 바꿨다.

## 낡은 카드(archive-v4-stale) 대비 바꾼 줄 (diff 줄 수: `<`/`>` 각 23, 총 46)

| 파일 | 줄 | 내용 |
|---|---|---|
| 1-question.md | 1 | 제목 10분 → 5분 경계 |
| 1-question.md | 5 | 경계 예시 `10:10:00`,`10:15:00` → `10:15:00`,`10:17:00` |
| 1-question.md | 8 | 고치는 방법 직전/다음 10분 → 5분 경계 |
| 2-summary.md | 8 | 흐름도 분 % 10 → 분 % 5 |
| 2-summary.md | 18 | `규칙 (pushq 2.3.x 기준)` → `규칙 (pushq 2.8.x 기준)` (버전) |
| 2-summary.md | 20, 24, 30 | 조건·교정·요점 10 → 5 |
| 2-summary.md | 33 | "직전 10분 경계로 내린다" → "직전 경계로 내린다"(3차 원문) |
| 3-answer.md | 8, 13~15 | 판정 예시·600초 → 300초 |
| 3-answer.md | 25 | 600초 중 1초 → 300초 중 1초 |
| 3-answer.md | 29~30 | 최대 9분 59초 일찍/10분 늦게 → 4분 59초/5분 |
| 3-answer.md | 37~38 | 탐침 예시 `:10`,`:15`,`:17`·10의 배수 → `:15`,`:16`,`:17`·5의 배수 |
| 3-answer.md | 57 | `SLOT = 600` → `SLOT = 300` |
| 3-answer.md | 63 | 10분 경계 밖 → 5분 경계 밖 |
| 3-answer.md | 70 | 표 최대 9분 59초 → 4분 59초 |
| 3-answer.md | 75 | 결론 "직전 10분 경계로" → "직전 경계로"(3차 원문) |
| metadata.md | 7 | 벤더 버전 pushq 2.3.x → pushq 2.8.x |

## 3차 카드와 규칙 의미가 같다는 근거

- `diff -r archive-v3/… archive-v5-fresh/…` 차이는 날짜 3곳(1-question 4행 예시 날짜 2024-08-03, 3-answer 64행 검증 기록 2024-08-07, metadata 초안 2024-08-07)과 버전 표기 2곳(2-summary 18행 머리, metadata 벤더 버전 행)·metadata 사건 행뿐이다. 규칙·대처·예시·코드 줄은 3차와 바이트 단위로 같다.
- 요약 `summary-fresh.txt` 는 3차 `summary.txt` 와 "pushq(2.8 기준)" 한 곳만 다르다.
- 확인: cases-v4 사본 + fix.patch(3차와 cmp 동일) + hidden → `hidden: all passed`, exit 0. fix.patch 의 `PUSH_SLOT_SECONDS = 300`·직전 경계 floor 가 카드 대처(직전 5분 경계로 내림, `SLOT = 300`)와 일치. `--version` = `pushq 2.8.0`.
