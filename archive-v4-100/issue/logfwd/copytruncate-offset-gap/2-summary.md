# issue/logfwd/copytruncate-offset-gap — 정리 (힌트)

태그: `silent-failure` · `environment-drift`

## 전체 흐름

```
00:00 logrotate copytruncate
  app.log (800MB) ──복사──▶ app.log.1
  app.log ──truncate──▶ 0 바이트 (inode 동일)

logfwd: inode 같음 → "같은 파일" → 오프셋 800MB 유지
        size(0) < offset(800MB) → 새 데이터 없음으로 판단, 대기
        ...
        size 가 800MB 를 넘는 순간 → 800MB 이후만 읽음
        → 0 ~ 800MB 구간(새로 쓴 로그 전부) 유실

공백 길이 ≈ 전날 하루치 로그 크기를 다시 채우는 시간
교정: 회전 = create + 앱 reopen (또는 logfwd truncate_detect on)
```

## 핵심 문장

- `logfwd`는 **inode가 같으면 같은 파일**로 보고, 파일이 오프셋보다 작아져도 처음부터 읽지 않고 **예전 오프셋까지 자랄 때를 기다린다**.
- `copytruncate`는 inode를 유지한 채 0으로 자르므로 이 동작과 만나면 **회전 직후 구간이 통째로** 빠진다.
- 공백은 전날 로그 크기만큼 다시 쌓이는 데 걸리는 시간이다 — 전날이 바쁘고 오늘이 한가할수록 길다.
- 대처는 회전 방식을 `create`로 바꾸고 앱이 파일을 다시 열게 하거나, 에이전트의 잘림 감지 옵션을 켜는 것.
- 감시는 "앱이 쓴 줄 수" 대 "수집된 줄 수"를 시간대별로 비교한다.
