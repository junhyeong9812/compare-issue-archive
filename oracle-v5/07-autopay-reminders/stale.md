# stale — 07-autopay-reminders (4차 낡은 카드 기록)

## 규칙과 버전

| | 규칙 | 교정 | 버전 |
|---|---|---|---|
| 현재(바이너리) | `schedule --at` 이 5분 경계(분 5의 배수·초 00, 300초)가 아니면 티켓·exit 0 이지만 큐 미적재 | 직전 5분 경계로 내림 | `pushq 2.8.0` (`--version`, 3차부터 있던 값 유지) |
| 낡은(카드·summary-stale) | 10분 경계(분 10의 배수·초 00, 600초)가 아니면 큐 미적재 | 직전 10분 경계로 내림 | `pushq 2.3.x` (카드 metadata `벤더 버전`, 2-summary "규칙 (pushq 2.3.x 기준)"), 사건 2024-08 |

- 같은 메커니즘(경계 밖 예약 = 티켓만 발급)에서 경계 폭만 다르다. 10분 경계는 5분 경계의 부분집합이라 낡은 교정도 **모두 적재·발송된다** — 오류 없이 최대 9분 59초 일찍 도착할 뿐이라 조용히 틀린다.

## stale-fix 결과 (2026-10-09, cases-v4 사본 + stale-fix.patch + hidden)

- `bash hidden.sh` → exit 1 (실패, 의도대로)
  ```
  FAIL - a whole billing day with odd merchant times and retries reaches everyone in time: as of 2026-12-07T09:13:05: pushes received by M7002: expected ["2026-12-07T07:35:00"], got ["2026-12-07T07:30:00"]
  FAIL - the original billing-day sample delivers every reminder no later than promised: sample: pushes received by M7003: expected ["2026-10-12T13:15:00"], got ["2026-10-12T13:10:00"]
  hidden: 2 failed
  ```
- `bash test.sh` → exit 0 (통과 — 보이는 테스트는 도착 여부만 보고 정확한 시각은 보지 않는다. 3차 우회 m8 "10분 내림 PASS/FAIL"과 같음)

## 낡음을 알아챌 단서 (탐침, cases-v4 폴더, `export PUSHQ_HOME=$(mktemp -d)`)

```
$ vendor/bin/pushq --version
pushq 2.8.0                                   ← 카드는 2.3.x 기준
$ vendor/bin/pushq schedule --to d1 --at 2026-10-12T13:15:00 --tag a x
ticket T000001
$ vendor/bin/pushq schedule --to d1 --at 2026-10-12T13:17:00 --tag b x
ticket T000002
$ vendor/bin/pushq list
T000001	2026-10-12T13:15:00	d1	a          ← 카드대로면 빠져야 할 :15:00 이 적재됨 → 경계는 10분이 아니라 5분
```

