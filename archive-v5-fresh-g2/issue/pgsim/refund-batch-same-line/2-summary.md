# issue/pgsim/refund-batch-same-line — 정리 (힌트)

> 유효: pgsim 4.1.x — 확인: `pgsim --version`

태그: `silent-failure` · `identity` · `contract-drift`

## 전체 흐름 (pgsim 4.1.x 기준)

```
해지 배치 ──refund --batch B FILE──▶ pgsim
   FILE                                  │ 한 배치 안에서 (TID, 금액, AUX) 비교
   T1  9900  (AUX 없음)  ─┐              │   첫 줄  → 환불 (RF 번호 발급)
   T1  9900  (AUX 없음)  ─┘ 같은 3칸      │   뒤 줄  → 조용히 버림
   T2  9900  (AUX 없음)     TID 다름      │   → 환불
                                         └─ stdout "accepted 3"(파일 줄 수), exit 0, stderr 없음

조회로만 보인다
  status --batch B : RF 2줄 (T1 9900, T2 9900)   ← 보낸 3줄과 불일치
  txn T1           : refunded 9900               ← 장부는 19800

같은 3칸 판정
  금액 다름        → 둘 다 환불
  AUX 다름         → 둘 다 환불
  AUX 둘 다 빈 값  → 같은 줄 (1건만)
  배치(호출)가 다름 → 비교 안 함 (둘 다 환불)
  같은 배치 번호 재전송 → 아무것도 안 함, 그래도 "accepted N"

교정
  줄마다 AUX = 결정적 줄 식별자 (주문번호-회차/상품줄 번호 등, [A-Za-z0-9-] ≤24)
  완료 판정 = status 의 RF 건수·txn 의 refunded 대사 (accepted 아님)
```

## 핵심 문장

- `pgsim refund`는 한 배치 안에서 **TID·금액·AUX가 모두 같은 줄**을 재전송으로 보고 **첫 줄만** 환불한다 — 오류·경고가 없다.
- `accepted N`은 **파일 줄 수**다 — 환불 건수가 아니다. 실제 환불은 `status --batch`의 RF 줄과 `txn`의 refunded로만 보인다.
- AUX를 비우면 빈 값끼리 같다고 본다 — **같은 가격 항목 여러 개를 한 원거래에서 한 번에 환불**하는 경우만 걸린다.
- 교정은 줄마다 **서로 다른 결정적 AUX**(줄 식별자)를 넣는 것이다 — 배치·금액 구성은 그대로 둔다.
- 같은 배치 번호 재전송은 무시된다 — 누락분 재처리는 새 배치 번호로 하고, 대사는 조회 결과 기준으로 한다.
