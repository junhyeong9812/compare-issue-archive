# issue/kvcache/resharding-ask-redirect-unhandled — 정리 (힌트)

태그: `client-compat` · `deploy`

## 전체 흐름

```
slot 5461 이전 중:  node-A (migrating) → node-D (importing)
GET sess:abc → node-A: 키 이미 이동 → "-ASK 5461 node-D"
  ├ 결제 서비스 클라이언트(신버전): ASKING + GET → node-D 정상
  └ 세션 서비스 클라이언트(2018년판): ASK 를 오류로 반환 → 500
```

## 핵심 문장

- 슬롯 이전 중에는 이미 옮겨진 키에 대해 **ASK** 리다이렉트가 온다 — 영구 이동(MOVED)과 다르다.
- ASK는 `ASKING` 명령을 먼저 보내야 하는데, 구버전 클라이언트는 이를 처리하지 못하고 오류로 낸다.
- 이전이 끝나면 MOVED로 바뀌고 클라이언트가 슬롯표를 갱신해 회복된다.
- 증설 전 서비스별 클라이언트 버전 목록을 점검한다.
