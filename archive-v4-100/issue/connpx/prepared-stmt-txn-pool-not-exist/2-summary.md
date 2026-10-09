# issue/connpx/prepared-stmt-txn-pool-not-exist — 정리 (힌트)

태그: `environment-drift` · `config`

## 전체 흐름

```
앱 커넥션 A ─(5회째 이후) PARSE S_3 ─▶ connpx ─▶ 백엔드 #2 에 S_3 생성
앱 커넥션 A ─EXECUTE S_3───────────▶ connpx ─▶ 백엔드 #5 (S_3 없음)
                                               └ ERROR: prepared statement "S_3" does not exist
```

## 핵심 문장

- 드라이버는 `prepareThreshold`(기본 5)를 넘으면 **서버 측 prepared statement**를 만든다 — 그 이름은 만든 백엔드 커넥션에만 있다.
- 트랜잭션 풀링은 트랜잭션마다 백엔드를 바꾸므로, 다음 실행이 **다른 백엔드**로 가면 이름이 없다.
- 백엔드 수가 적은 스테이징에선 같은 백엔드로 갈 확률이 높아 드러나지 않았다.
- 대처는 드라이버 `prepareThreshold=0`(서버 측 prepare 끔) — 프록시가 prepare 추적을 지원하는 버전이면 그 기능을 켠다.
