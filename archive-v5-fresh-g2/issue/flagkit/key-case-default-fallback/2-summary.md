# issue/flagkit/key-case-default-fallback — 정리 (힌트)

태그: `contract-drift` · `silent-failure` · `environment-drift`

## 전체 흐름

```
관리 콘솔  ── 저장: "Payment.NewRouting" (대소문자 보존) ──▶ 플래그 저장소 ──스냅샷──▶ SDK 캐시

서비스 A (SDK 2.x)  bool("Payment.NewRouting") ── 그대로 조회 ──▶ 찾음 → true
서비스 B (SDK 3.x)  bool("Payment.NewRouting") ── lower() ──▶ "payment.newrouting"
                                                              └▶ 못 찾음 → 기본값 false
                                                                 DEBUG "miss key" (기본 INFO 에선 안 보임)

TTL 단축      → 스냅샷만 더 자주 받음, 결과 동일 (효과 없음)
소문자 키 추가 → 두 키가 따로 놀아 버전별로 다른 값을 볼 위험

교정: 키 규칙 = 소문자 kebab/dot, 콘솔 생성 시 검사, SDK onMiss 훅 → 메트릭
```

## 핵심 문장

- `flagkit` SDK **3.x는 조회 키를 소문자로 바꾼 뒤** 찾는다 — 콘솔은 대소문자를 보존해 저장하므로 대문자가 섞인 키는 3.x에서 **항상 기본값**이 된다.
- 2.x는 키를 그대로 찾는다 — 같은 플래그가 SDK 버전에 따라 다르게 보인다.
- 못 찾은 키는 DEBUG 로그에만 남는다 — 기본 INFO에서는 흔적이 없다.
- "반영 지연"이면 시간이 지나면 바뀌고, "키 불일치"면 **영원히 기본값**이다 — 기다려도 안 바뀌면 키를 의심한다.
- 교정은 키 이름 규칙(소문자)과 콘솔 검사, 그리고 miss를 메트릭으로 내보내는 것.
