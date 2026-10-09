# issue/outboxd/sequence-commit-order-skip — 정리 (힌트)

태그: `silent-failure` · `concurrency`

## 전체 흐름

```
트랜잭션 T1: outbox insert id=1001 ... (긴 처리) ... commit 10:00:02
트랜잭션 T2: outbox insert id=1002 → commit 10:00:01
outboxd 폴링 10:00:01.5: SELECT ... WHERE id > 1000 → 1002 만 보임 → 발행, last_id=1002
10:00:02 T1 커밋 → 1001 은 영원히 id > 1002 조건에 안 걸림
```

## 핵심 문장

- 시퀀스 번호는 **발급 순서**이지 **커밋 순서**가 아니다 — 작은 id가 나중에 보일 수 있다.
- `outboxd`는 "마지막 id 이후"만 읽어 늦게 보인 작은 id를 건너뛰었다.
- 결제 트랜잭션이 길어진 날(외부 호출 지연) 하루 수십 건씩 빠졌다.
- 대처: 발행 여부를 행의 `published` 상태로 관리(미발행 전부 조회), 하류는 이벤트 id로 멱등.
