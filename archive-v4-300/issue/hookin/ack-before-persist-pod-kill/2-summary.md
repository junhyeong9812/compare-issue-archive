# issue/hookin/ack-before-persist-pod-kill — 정리 (힌트)

태그: `silent-failure` · `durability`

## 전체 흐름

```
PG ──POST /notify──▶ hookin
   handler: response.send(200)  → 비동기로 queue.publish(body)
   배포 롤링: SIGTERM → 30초 유예 없이 종료(preStop 미설정)
   200 이 나간 요청의 publish 가 완료 전 중단
PG: 200 받았으므로 재전송 안 함
```

## 핵심 문장

- PG는 **200을 받으면 재전송하지 않는다** — 수신 확인을 보내는 순간 책임이 우리에게 넘어온다.
- `hookin`은 응답 속도를 위해 200을 먼저 보내고 적재를 비동기로 했다.
- 배포·축소로 파드가 종료될 때 미적재분이 사라졌다 — 배포 시간대에만 "가끔, 일부" 빠졌다.
- 대처: 적재(내구 저장) 완료 후 200, 종료 시 진행 중 요청 마무리(graceful shutdown), 일 1회 PG 거래 내역과 대사.
