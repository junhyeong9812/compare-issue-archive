# issue/notifyhub/collapse-key-offline-last-only — 정리 (힌트)

태그: `silent-failure` · `delivery-semantics`

## 전체 흐름

```
결제 3건 (10:01, 10:03, 10:05) → notifyhub → 플랫폼 (collapse_key = "PAYMENT")
단말 오프라인(지하철) 10:00~10:10
   플랫폼 보관: 같은 collapse key 는 최신 1건만 유지
10:10 단말 접속 → 10:05 알림만 표시
```

## 핵심 문장

- collapse key가 같은 메시지는 플랫폼이 **오프라인 동안 마지막 1건만** 보관한다 — 덮어쓰기는 오류가 아니다.
- `notifyhub`가 알림 **종류**(PAYMENT)를 collapse key로 넣어, 서로 다른 결제 알림이 서로를 덮었다.
- 온라인 단말은 3건 모두 받으므로 "일부 고객·일부 시점에만" 누락으로 보였다.
- 대처: 결제 알림은 collapse key를 쓰지 않고, 덮어써도 되는 알림(잔액 갱신 등)에만 쓴다.
