# issue/stockpush/zero-qty-noop — 정답

태그: `silent-failure` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **아무것도 바뀌지 않는다.** `show`는 qty 5, st 1, 그리고 이전 push의 transfer 번호를 그대로 보여 준다.\
`push`는 0 줄도 줄 수에 넣어 `T0000002	1`을 출력하고 exit 0으로 끝나지만, 그 줄을 "변경 없음"으로 취급해 저장하지 않는다.\
벤더 `--help`에는 이 동작이 없고 stderr도 비어 있다.
   > **transfer** — 그 상품을 마지막으로 바꾼 push의 번호. 무시된 줄은 이 번호를 갱신하지 않으므로, 반영 여부를 가르는 단서가 된다.

2. **처음 보는 SKU는 생기지 않는다.** 0 줄은 새 SKU에 대해서도 무시되므로 `show`는 E31(없음)이다.\
"0"과 "00"은 같은 0으로 무시되고, "-1"이나 "2.5"는 E15(exit 3)로 파일 전체가 거부된다.\
"-"는 qty 0·st 2로 저장되며, 처음 보는 SKU도 qty 0·st 2로 생긴다.

3. **0이 무시됐기 때문이다.** 창고 이전 기간에 판매를 멈추려고 전 상품을 수량 0으로 보냈다.\
push는 exit 0과 전체 줄 수를 돌려줬고, 배치는 그것을 "판매 중지 완료"로 기록했다.\
마켓은 모든 상품을 예전 수량 그대로 판매 중으로 두었고, 주문이 계속 들어와 출고 불가 취소와 마켓 패널티가 쌓였다.\
발견은 마켓 운영팀 통보("취소율 급증")였다.

4. **정상 복귀한다.** `-` 다음에 양수를 보내면 qty가 그 값으로, st가 1로 바뀐다 — `-`는 되돌릴 수 있는 품절 표시다.\
`-`를 "알 수 없음·건너뜀"으로 이해하면, 수량을 모르는 상품에 `-`를 넣어 멀쩡한 상품을 품절시키거나, 품절에는 0을 계속 보내는 두 가지 실수를 한다.

5. **0이 "수량 0"이라는 상식 때문이다.** 도움말은 "QTY: digits or -"라고만 하므로 0은 digits에 포함된 정상 값으로 보인다.\
실제 반영은 `list`/`show`의 qty·st·transfer로만 보인다 — 0을 보낸 상품은 transfer가 예전 번호로 남아 있다.

6. **피드는 0 대신 `-`, 확인은 조회로.** 판매 가능 수량을 계산한 뒤 0 이하이면 QTY 자리에 `-`를 쓴다(음수를 그대로 보내면 E15).\
push 후에는 보낸 SKU마다 `list`의 qty가 보낸 값(또는 `-`면 0·st 2)과 같은지 대사해서, 규칙이 바뀌거나 다른 경로로 0이 새어 나가는 경우를 잡는다.

## 문제 구조 (추상화 코드)

### 변형 A — 판매 중지를 수량 0으로 보냄 (창고 이전 일괄 중지)
① 문제 코드
```java
for (Product p : catalog) lines.add(p.sku() + "\t" + 0);   // 전 상품 판매 중지 의도
Transfer t = stockpush.push(lines);                        // exit 0, 줄 수 = 전체
log.info("paused {} items", t.lines());                    // 실제로는 0건 반영
```
② 고친 코드
```java
static final String SOLD_OUT = "-";                         // 벤더의 0 표기
for (Product p : catalog) lines.add(p.sku() + "\t" + SOLD_OUT);
stockpush.push(lines);
```
무엇이 깨졌나: 벤더는 0을 "변경 없음"으로 해석하는데 호출부는 0을 "수량 0"으로 보냈고, 성공 응답(exit 0·줄 수)을 반영 완료로 믿었다.

### 변형 B — 일상 피드에서 계산된 0을 그대로 씀
① 문제 코드
```java
int qty = Math.max(p.onHand() - p.reserved() - safety, 0);
lines.add(p.sku() + "\t" + qty);                            // 0 이면 무시됨
```
② 고친 코드
```java
int qty = Math.max(p.onHand() - p.reserved() - safety, 0);
lines.add(p.sku() + "\t" + (qty > 0 ? String.valueOf(qty) : SOLD_OUT));
```
무엇이 깨졌나: 계산 결과가 0이 되는 순간에만 벤더 규칙에 걸려, 품절 상품만 예전 수량으로 남았다.

### 변형 C — push 응답으로 반영을 판정함 (대사 누락)
① 문제 코드
```java
Transfer t = stockpush.push(lines);
if (t.lines() == lines.size()) markSynced(skus);            // 줄 수 = 반영 수라고 가정
```
② 고친 코드
```java
Transfer t = stockpush.push(lines);
Map<String, Row> market = stockpush.list().bySku();
for (String sku : skus)
    if (!market.get(sku).transfer().equals(t.id())) alert("not applied: " + sku);
```
무엇이 깨졌나: 줄 수는 "읽은 줄"이지 "바뀐 상품"이 아니다 — 반영은 조회 결과(transfer 갱신)로만 확인된다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 0이면 `-` 전송 | 벤더 표기(`-`)를 안다 | 작음 | 표기가 바뀌면 다시 무시 | 모든 피드 호출부 — 기본 |
| 0 대신 1 전송 후 마켓에서 수동 중지 | 마켓 콘솔 접근 | 큼(수작업) | 1개가 팔림 | 선택하지 않은 방법 |
| push 후 list/show 대사 + 알림 | 조회 권한 | 중간(호출 수 증가) | 무시 자체는 막지 못함 | `-` 전송과 함께 안전망으로 |

**결론**: `-` 전송이 근본 대처이고, transfer 갱신 여부 대사를 안전망으로 함께 둔다.

## 검증 기록
- 2024-05-14: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
