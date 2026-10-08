# probe — 바이너리 실행만으로 규칙을 알아내는 경로 (명령 10개)

케이스 폴더에서 실행. 출력은 실제 실행 원문(2026-10-09).

```
$ export PUSHQ_HOME=$(mktemp -d)                                                        # 1
$ vendor/bin/pushq --help                                                               # 2
usage:
  pushq schedule --to <device> --at <YYYY-MM-DDTHH:MM:SS> [--tag <tag>] <message>
  pushq send --to <device> [--tag <tag>] <message>
  pushq list [--to <device>]          print <ticket>TAB<at>TAB<device>TAB<tag>
  pushq run --until <YYYY-MM-DDTHH:MM:SS>
  pushq log [--to <device>]           print <at>TAB<device>TAB<tag>TAB<message>
  pushq cancel <ticket>
limits: device 1..64, tag 0..64, message 1..512 bytes
env: PUSHQ_HOME (state directory, required)

# 앱이 실제로 만드는 시각(13:47 출금 − 30분) 과 5분 단위 시각
$ vendor/bin/pushq schedule --to ios:Zx81kQpLm0Ty --at 2026-10-12T13:17:00 --tag a x   # 3
ticket T000001
$ vendor/bin/pushq schedule --to ios:Zx81kQpLm0Ty --at 2026-10-12T13:15:00 --tag b x   # 4
ticket T000002
$ vendor/bin/pushq list                                                                 # 5
T000002	2026-10-12T13:15:00	ios:Zx81kQpLm0Ty	b        ← T000001(13:17) 은 티켓만 있고 큐에 없음

# 경계 탐색: 초가 있는 시각 / 다른 5분 배수 / 5의 배수가 아닌 분
$ vendor/bin/pushq schedule --to d1 --at 2026-10-12T13:20:30 --tag c x                 # 6
ticket T000003
$ vendor/bin/pushq schedule --to d1 --at 2026-10-12T13:10:00 --tag d x                 # 7
ticket T000004
$ vendor/bin/pushq schedule --to d1 --at 2026-10-12T13:16:00 --tag e x                 # 8
ticket T000005
$ vendor/bin/pushq list                                                                 # 9
T000004	2026-10-12T13:10:00	d1	d
T000002	2026-10-12T13:15:00	ios:Zx81kQpLm0Ty	b        ← 13:20:30·13:16 도 빠짐 — 분%5==0 이고 초 00 인 것만 남음

$ vendor/bin/pushq run --until 2026-10-12T14:00:00 && vendor/bin/pushq log             # 10
delivered 2
2026-10-12T13:10:00	d1	d	x
2026-10-12T13:15:00	ios:Zx81kQpLm0Ty	b	x                ← 시각이 지나도 빠진 티켓은 발송되지 않음
```

(선택 확인) `vendor/bin/pushq cancel T000001` → `pushq: E31`, exit 2 — 발급된 티켓이 큐에 존재하지 않는다.

결론: `schedule --at` 이 5분 경계(분이 5의 배수, 초 00)가 아니면 티켓 번호를 주고 exit 0 이지만 큐에 넣지 않는다.
코드만 읽어서는 알 수 없다(래퍼는 티켓 번호를 성공으로 믿고, 리포트는 벤더 발송 기록만 본다).
교정: 예약 시각을 직전 5분 경계로 내린다(늦게 보내지 않기 — 올림·반올림은 설정 시점보다 늦어짐).
