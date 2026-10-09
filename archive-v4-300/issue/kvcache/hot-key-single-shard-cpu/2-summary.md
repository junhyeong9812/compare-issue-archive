# issue/kvcache/hot-key-single-shard-cpu — 정리 (힌트)

태그: `performance` · `hot-spot`

## 전체 흐름

```
GET coupon:event:1111 × 120k/s → slot 9012 → node-3 하나
node-3 CPU 100% / 나머지 5개 15%
```

## 핵심 문장

- 클러스터는 **키 단위**로 슬롯을 나누므로 키 하나에 몰린 요청은 노드를 늘려도 한 노드로 간다.
- 해법은 키를 복제해 나누거나(`key#0..N`), 앱 로컬 캐시로 흡수하는 것이다.
- 읽기 복제본 분산은 클라이언트가 복제본 읽기를 지원해야 하고, 그래도 노드 수배 이상은 못 늘린다.
- `kvcache hotkeys`(LFU 기반)로 이벤트 전에 집중 키를 찾는다.
