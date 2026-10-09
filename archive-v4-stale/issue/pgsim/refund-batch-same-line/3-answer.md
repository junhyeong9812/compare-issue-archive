# issue/pgsim/refund-batch-same-line — 정답

태그: `silent-failure` · `identity` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **응답은 `accepted 2`·exit 0이지만 환불은 1건이다.** stderr도 비어 있다.\
`status --batch`에는 RF 번호가 붙은 줄이 하나만 나오고, `txn TID`의 `refunded`도 9900만 늘어난다.\
벤더 `--help`는 AUX를 "optional"이라고만 적고 이 동작은 적지 않는다.
   > **AUX** — 환불 줄의 셋째 칸. 가맹점이 자유롭게 쓰는 값으로, PG 정산 파일에 그대로 찍힌다. 같은 줄 판정에는 쓰이지 않는다.

2. **TID와 금액이 같으면 합쳐진다.** 금액이 다르면 2건 환불된다.\
AUX만 다를 때(`O1-1`·`O1-2`)도, AUX를 둘 다 비웠을 때도 1건만 환불한다 — pgsim 3.3.x는 같은 줄 판정에 AUX를 보지 않는다.\
같은 두 줄을 배치 번호를 달리해 따로 보내면 2건 모두 환불된다 — 비교 범위는 한 호출(한 배치 파일) 안이다.

3. **배치가 보는 성공 신호(exit 0·accepted 건수)가 실제 환불 건수와 무관했다.** 2024년 4월 정기배송 해지 배치는 남은 회차마다 `TID<TAB>회차 금액` 줄을 한 배치에 넣었다.\
남은 회차가 2회 이상이면 금액이 같은 줄이 둘 이상 생겨 첫 회차만 환불됐고, 남은 회차가 1회인 해지는 같은 줄이 생길 수 없어 늘 맞았다.\
배치는 accepted 건수만 확인하고 해지 건을 "환불 완료"로 표시했으므로, 가맹점 정산금이 장부보다 많게 들어온 월말 PG 정산 대사에서야 차액이 드러났다.

4. **아무것도 하지 않는다.** 이미 처리한 배치 번호가 다시 오면 `pgsim`은 파일을 보지 않고 `accepted N`만 낸다(exit 0).\
그래서 누락 줄만 같은 배치 번호로 다시 보내면 또 조용히 무시된다.\
재처리는 새 배치 번호로 하고, 그 배치 안에서도 같은 (TID, 금액) 줄이 둘 이상 들어가지 않게 해야 한다.

5. **AUX를 채우는 것으로는 막을 수 없고, 합치면 항목별 증빙이 깨진다.** AUX는 판정에 쓰이지 않으므로 줄 식별자를 넣어도 여전히 첫 줄만 환불된다(변형 B — 배송비 환불이 바로 이 경우였다).\
금액을 합친 한 줄은 RF 번호가 하나라 회차·상품별 환불 번호를 줄 수 없고, 부분 취소·이의 제기 때 금액을 다시 나눠야 한다.\
그래서 같은 (TID, 금액) 줄은 한 배치에 하나만 넣고, 겹치는 줄은 다음 배치(새 배치 번호)로 넘긴다. 겹치지 않는 줄은 그대로 한 배치에 둬서 호출 수 증가를 최소로 한다.

6. **accepted는 파일 접수 증빙일 뿐이다.** 완료 판정은 `status --batch`의 RF 줄 수가 보낸 줄 수와 같은지, 원거래별 `txn`의 refunded 증가분이 보낸 금액 합과 같은지로 한다.\
근본 대처는 같은 (TID, 금액) 줄을 배치마다 하나로 나눠 합쳐짐 자체를 막는 것이고, 조회 대사는 다른 경로에서 같은 일이 생길 때를 위한 안전망이다.

## 문제 구조 (추상화 코드)

### 변형 A — 같은 금액 회차를 한 배치로 환불 (정기배송 해지)
① 문제 코드
```java
for (Installment i : sub.remaining())                       // 남은 회차: 9900, 9900, 9900
    file.add(sub.tid() + "\t" + i.amount());                  // TID·금액이 같은 세 줄
int accepted = pg.refund(mid, batchId, file);                 // "accepted 3", exit 0 — 환불은 1건
if (accepted == file.size()) sub.markRefunded();              // 완료로 오인
```
② 고친 코드
```java
List<List<Line>> rounds = new ArrayList<>();                  // 한 배치에 같은 (TID, 금액)은 하나만
for (Line l : lines) {
    List<Line> slot = rounds.stream().filter(r -> r.stream().noneMatch(l::sameTidAmount)).findFirst().orElse(null);
    if (slot == null) rounds.add(slot = new ArrayList<>());
    slot.add(l);
}
for (int k = 0; k < rounds.size(); k++) pg.refund(mid, runId + "-" + (k + 1), rounds.get(k));
```
무엇이 깨졌나: PG는 배치 안의 (TID, 금액)이 같은 줄을 재전송으로 보는데, 호출부가 같은 금액 회차를 한 배치에 몰아 넣었다.

### 변형 B — AUX에 배송 건 식별자를 넣어도 합쳐짐 (배송비 환불)
① 문제 코드
```java
line.aux = order.no() + "-" + shipment.seq();                 // 줄마다 다른 AUX
// 묶음 배송 2건의 배송비 3000, 3000 → AUX가 달라도 같은 줄로 합쳐져 3000만 환불
```
② 고친 코드
```java
// AUX는 그대로 두고(정산 추적용), 같은 TID·같은 배송비 줄은 변형 A처럼 다음 배치로 넘긴다
```
무엇이 깨졌나: AUX가 줄을 구별해 줄 것이라고 가정했지만 PG의 같은 줄 판정은 TID와 금액만 본다.

### 변형 C — accepted로 완료를 판정함 (대사 누락)
① 문제 코드
```java
if (pg.refund(mid, b, file) == file.size()) markAllDone();    // accepted = 줄 수일 뿐
```
② 고친 코드
```java
pg.refund(mid, b, file);
List<Row> rows = pg.status(b);                                // RF 번호가 붙은 줄만 실제 환불
if (rows.size() != file.size()) alert("refund rows " + rows.size() + "/" + file.size() + " in " + b);
```
무엇이 깨졌나: 벤더 성공 신호를 실제 처리 결과 대신 썼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 겹치는 (TID, 금액) 줄만 다음 배치로 | 규칙(같은 TID·금액 → 1건)을 안다 | 작음(겹칠 때만 배치 +1) | 배치 번호 규칙(회차-순번)을 정산 대사와 맞춰야 함 | 모든 환불 배치 — 기본 |
| 줄마다 다른 AUX | AUX가 판정에 쓰인다고 가정 | 작음 | 그대로 합쳐짐(변형 B) | 맞지 않음 |
| 같은 TID 줄을 금액 합계 1줄로 | 항목별 환불 번호가 필요 없음 | 작음 | 항목별 증빙·부분 이의 처리 불가 | 선택하지 않은 방법 |
| status/txn 사후 대사 | 조회 권한 | 중간 | 합쳐짐 자체는 막지 못함 | 배치 분리와 함께 안전망 |

**결론**: 같은 (TID, 금액) 줄을 배치마다 하나로 나눠 보내는 것이 근본 대처이고, status/txn 대사는 같은 종류의 조용한 누락을 잡는 안전망이다. 둘을 함께 둔다.

## 검증 기록
- 2024-05-02: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
