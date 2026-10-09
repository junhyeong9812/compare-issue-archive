# issue/replwatch/failover-old-primary-still-writable — 정리 (힌트)

태그: `split-brain` · `data-loss`

## 전체 흐름

```
          promote             VIP 이동
replica-2 ──────▶ 새 primary ◀── 새 커넥션
old primary (회복, read-write 그대로) ◀── 인스턴스 #3 의 기존 커넥션 (VIP 이동 전 연결)
                     └ 30분간 주문 212건 기록 → 새 primary 에는 없음
```

## 핵심 문장

- `promote`는 새 주 DB를 올릴 뿐, **옛 주 DB를 읽기 전용으로 만들거나 끄지 않는다** — `replwatch fence`는 별도 단계다.
- VIP 이동은 **새 연결**에만 영향을 준다 — 이미 맺은 TCP 커넥션은 옛 서버에 남는다.
- 증상은 '일부 주문만 사라짐'이다 — 그 인스턴스로 간 요청만 영향을 받는다.
- 복구는 옛 주 DB의 장애 조치 시각 이후 WAL·행을 새 주 DB와 비교해 옮긴다.
