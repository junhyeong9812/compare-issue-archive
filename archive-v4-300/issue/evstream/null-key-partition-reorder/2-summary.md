# issue/evstream/null-key-partition-reorder — 정리 (힌트)

태그: `ordering` · `data-semantics`

## 전체 흐름

```
order-svc: send(topic=order.status, key=null, value={orderId, status})
  PAID     → 파티션 3 (지연 중인 브로커)
  PENDING  → 파티션 7   ← 실제로는 PENDING 이 먼저 발행됨
조회 서비스 소비: PAID(3) 가 PENDING(7) 보다 늦게 도착할 수도, 먼저 도착할 수도 있음
  먼저 PAID 처리 → 나중 PENDING 처리 → 최종 '결제 대기'
```

## 핵심 문장

- 순서는 **파티션 안에서만** 보장된다 — 키가 없으면 생산자가 파티션을 돌려 쓰므로 같은 주문의 이벤트가 갈린다.
- 리팩터링에서 메시지 빌더가 바뀌며 `key(orderId)`가 빠졌다 — 컴파일·테스트는 통과했다.
- 평소에는 파티션 간 지연 차이가 작아 드물었고, 브로커 하나가 느려진 날 0.4%의 주문이 역전됐다.
- 대처: 주문 id를 키로 복구, 소비자는 상태 버전(`seq`)이 더 낮은 이벤트를 버리도록 방어.
