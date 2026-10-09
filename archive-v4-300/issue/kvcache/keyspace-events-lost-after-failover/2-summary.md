# issue/kvcache/keyspace-events-lost-after-failover — 정리 (힌트)

태그: `environment-drift` · `silent-failure`

## 전체 흐름

```
old master: notify-keyspace-events = Ex (런타임 CONFIG SET, 파일 미저장)
replica  : notify-keyspace-events = "" (기본)
장애 조치 → replica 승격 → 만료 이벤트 발행 안 함
워커: 구독은 살아 있음, 메시지 0건 → 오류 없음
hold:* 키는 만료로 사라지지만 재고 테이블의 hold_qty 는 그대로
```

## 핵심 문장

- `CONFIG SET`은 **그 노드의 런타임**만 바꾼다 — 복제본·설정 파일에는 반영되지 않는다(`CONFIG REWRITE`·관리 템플릿 필요).
- 만료 이벤트는 '보장된 전달'이 아니다 — 구독이 끊기거나 설정이 꺼져도 **아무도 알리지 않는다**.
- 키는 만료되지만, 이벤트로 하던 **후처리(재고 반환)**가 일어나지 않는다.
- 정리 작업은 이벤트 대신 **만료 시각 인덱스 주기 스캔**을 정본으로 둔다.
