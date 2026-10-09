# issue/connpx/new-db-user-default-pool-size — 정리 (힌트)

태그: `config` · `performance`

## 전체 흐름

```
앱 (Hikari max 30) ──▶ connpx [pool settle_rw/pay : default_pool_size=5]
                              ├ 5개 사용 중
                              └ 25개 대기열 (cl_waiting) ── 평균 대기 2.8초
DB CPU 20% ← 일을 못 받고 있음
```

## 핵심 문장

- `connpx`의 풀은 **(사용자, DB) 쌍마다** 만들어지고, 사용자별 설정이 없으면 `default_pool_size=5`를 쓴다.
- 지연은 DB가 아니라 **프록시 대기열**에서 생긴다 — `SHOW POOLS`의 `cl_waiting`·`maxwait`로 보인다.
- 앱 풀을 늘리면 대기열만 길어진다.
- 계정을 바꾸면 프록시 사용자 설정·DB 권한·모니터링 라벨을 같이 옮긴다.
