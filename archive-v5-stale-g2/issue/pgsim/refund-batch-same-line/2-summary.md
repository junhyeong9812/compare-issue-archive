# issue/pgsim/refund-batch-same-line — 정리 (힌트)

> 유효: pgsim 3.3.x — 확인: `pgsim --version`

태그: `silent-failure` · `identity` · `contract-drift`

## 전체 흐름 (pgsim 3.3.x 기준)

```
해지 배치 ──refund --batch B FILE──▶ pgsim
   FILE                                  │ 한 배치 안에서 (TID, 금액) 비교 — AUX는 보지 않는다
   T1  9900  O1-1        ─┐              │   첫 줄  → 환불 (RF 번호 발급)
   T1  9900  O1-2        ─┘ TID·금액 같음 │   뒤 줄  → 조용히 버림 (AUX가 달라도)
   T2  9900  O2-1           TID 다름      │   → 환불
                                         └─ stdout "accepted 3"(파일 줄 수), exit 0, stderr 없음

조회로만 보인다
  status --batch B : RF 2줄 (T1 9900, T2 9900)   ← 보낸 3줄과 불일치
  txn T1           : refunded 9900               ← 장부는 19800

같은 줄 판정
  금액 다름        → 둘 다 환불
  AUX만 다름       → 같은 줄 (1건만)
  AUX 둘 다 빈 값  → 같은 줄 (1건만)
  배치(호출)가 다름 → 비교 안 함 (둘 다 환불)
  같은 배치 번호 재전송 → 아무것도 안 함, 그래도 "accepted N"

교정
  한 배치에 같은 (TID, 금액) 줄은 하나만 — 겹치는 줄은 새 배치 번호의 다음 배치로 넘긴다
  완료 판정 = status 의 RF 건수·txn 의 refunded 대사 (accepted 아님)
```

## 핵심 문장

- `pgsim refund`는 한 배치 안에서 **TID·금액이 같은 줄**을 재전송으로 보고 **첫 줄만** 환불한다 — 오류·경고가 없다.
- AUX(셋째 칸)는 정산 파일에 찍히는 메모일 뿐 **같은 줄 판정에 쓰이지 않는다** — AUX를 줄마다 다르게 채워도 합쳐진다.
- `accepted N`은 **파일 줄 수**다 — 환불 건수가 아니다. 실제 환불은 `status --batch`의 RF 줄과 `txn`의 refunded로만 보인다.
- **같은 가격 항목 여러 개를 한 원거래에서 한 번에 환불**하는 경우만 걸린다.
- 교정은 **같은 (TID, 금액) 줄을 서로 다른 배치로 나눠 보내는 것**이다 — 겹치는 줄만 다음 배치로 넘기고, 금액은 합치지 않는다.
- 같은 배치 번호 재전송은 무시된다 — 누락분 재처리도 새 배치 번호로 하고, 대사는 조회 결과 기준으로 한다.
