# issue/ledgerctl/memo-byte-silent-hold — 정답

태그: `silent-failure` · `encoding` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **나오지 않는다.** 한글 22자는 66바이트라 64바이트를 넘는다.\
`post`는 그래도 exit 0과 영수 번호를 출력하지만 전표는 st 4(보류)로 저장되고, `list`는 st 1인 전표만 출력한다.\
`show <영수번호>`는 전표 내용과 함께 `st 4`를 보여 준다 — 기장된 전표는 `st 1`이다.\
벤더 `--help`에는 이 동작이 없고 stderr도 비어 있어서, 호출부는 성공으로 받아들인다.
   > **보류(st 4)** — 원장에 저장은 됐지만 조회·집계 대상에서 빠지는 상태. 벤더 문서에 없는 동작이다.

2. **경계는 UTF-8 64바이트다(초과 시 보류, ledgerctl 4.6.x 기준).** ASCII 64자(64바이트)는 기장, 65자(65바이트)는 보류.\
한글 21자(63바이트)는 기장, 22자(66바이트)는 보류 — 22자는 글자 수로는 64보다 훨씬 짧다.\
그래서 글자 수 기준이 아니라 바이트 기준이고, 64바이트 "이하"는 기장된다.

3. **글자 수 검증이 바이트 경계를 보지 못했다.** 환불 배치는 메모(`환불 <주문번호> <고객명> <사유>`)를 40자로 자르고 있었다.\
영문·숫자 메모는 40자 = 40바이트라 경계 안이었지만, 한글 고객명·사유가 들어간 메모는 20자 남짓만 넘어도 64바이트를 넘었다.\
배치는 영수 번호를 받으면 "환불 기장 완료"로 표시했으므로 로그·종료 코드가 모두 정상이었고, 환불 대사 리포트(원장 `list` 기반)에서만 해당 건이 빠졌다.\
발견은 고객 문의("환불 영수 번호는 받았는데 입금 내역이 없다")였다.

4. **잘못된 UTF-8이 되어 E17로 거부된다.** 64바이트 지점이 한글(3바이트)·이모지(4바이트) 중간이면 마지막 문자가 쪼개진다.\
`ledgerctl`은 잘못된 UTF-8이나 제어 문자가 든 메모를 E17(exit 3)로 거부한다 — 이번엔 시끄럽게 실패하지만 전표가 아예 안 들어간다.\
올바른 자르기는 문자 단위로 바이트를 누적하다가 64를 넘기 직전에서 멈춘다(결과는 62~64바이트). 끝 공백은 정리해도 된다.
   > **문자 경계** — 멀티바이트 문자의 첫 바이트 앞. UTF-8에서 연속 바이트는 `10xxxxxx`이므로 그 앞에서는 자르면 안 된다.

5. **보류 건은 그대로 남는다.** 같은 참조 번호로 다시 post하면 새 영수 번호의 전표가 하나 더 생긴다(메모가 64바이트 이하면 st 1).\
원래 보류 건은 st 4로 남아 `list`에는 안 나오지만 `show`로는 보이므로, 재처리 후에는 영수 번호 기준 매핑을 새 번호로 바꿔야 한다.\
보류 건을 재처리할지는 원장 담당과 합의한다 — 이중 기장 위험은 없지만(보류는 집계 밖) 영수 번호 추적이 둘로 갈린다.

6. **영수 번호는 접수 증빙일 뿐이다.** `ledgerctl`은 기장 여부와 무관하게 영수 번호를 준다.\
배치는 post 후 같은 기간 `list`의 건수·합계를 보낸 건수·합계와 대사하거나, 영수 번호마다 `show`의 st가 1인지 확인해야 한다.\
더 근본적으로는 보내기 전에 64바이트 규칙을 지켜 보류 자체가 생기지 않게 한다 — 대사는 그 규칙이 깨졌을 때 알아채는 안전망이다.

## 문제 구조 (추상화 코드)

### 변형 A — 글자 수로만 자른 메모가 조용히 보류됨 (환불 배치)
① 문제 코드
```java
String memo = String.join(" ", "환불", orderNo, customerName, reason);
if (memo.length() > 40) memo = memo.substring(0, 40);     // 글자 수 기준
String receipt = ledger.post(account, amount, date, refundId, memo);  // 한글 메모 > 64B → st 4, 그래도 exit 0
refund.markBooked(receipt);                                // "기장 완료"로 오인
```
② 고친 코드
```java
String memo = String.join(" ", "환불", orderNo, customerName, reason);
if (memo.length() > 40) memo = memo.substring(0, 40);
memo = clipUtf8(memo, 64);                                 // 원장 메모 상한: UTF-8 64바이트
String receipt = ledger.post(account, amount, date, refundId, memo);

static String clipUtf8(String s, int maxBytes) {
    StringBuilder out = new StringBuilder();
    int used = 0;
    for (int i = 0; i < s.length(); ) {
        int cp = s.codePointAt(i);
        int n = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
        if (used + n > maxBytes) break;                    // 문자 경계에서 멈춤
        out.appendCodePoint(cp);
        used += n;
        i += Character.charCount(cp);
    }
    return out.toString().stripTrailing();
}
```
무엇이 깨졌나: 원장의 메모 상한은 UTF-8 바이트인데 호출부는 글자 수로만 잘랐다 — 한글 메모만 64바이트를 넘어 보류됐고, 영수 번호·exit 0 때문에 성공으로 기록됐다.

### 변형 B — 바이트열을 그냥 자른 교정이 E17로 거부됨 (같은 사건의 1차 수정)
① 문제 코드
```java
byte[] b = memo.getBytes(StandardCharsets.UTF_8);
byte[] cut = Arrays.copyOf(b, Math.min(b.length, 64));     // 문자 중간 절단 가능
ledger.postRaw(..., cut);                                  // 잘못된 UTF-8 → E17 (exit 3)
```
② 고친 코드
```java
memo = clipUtf8(memo, 64);                                 // 변형 A의 문자 경계 자르기
```
무엇이 깨졌나: 바이트 상한을 지키려고 바이트 단위로 잘라 멀티바이트 문자를 쪼갰다 — 보류 대신 거부로 바뀌었을 뿐 전표는 여전히 빠졌다.

### 변형 C — 영수 번호로 기장 완료를 판정함 (대사 누락)
① 문제 코드
```java
for (Refund r : batch) r.markBooked(ledger.post(...));    // 영수 번호 = 기장이라고 가정
report.total = sum(batch);                                 // 원장과 대사하지 않음
```
② 고친 코드
```java
for (Refund r : batch) r.setReceipt(ledger.post(...));
Map<String, Row> booked = ledger.list(from, to).byRef();   // 원장 조회로 판정
for (Refund r : batch)
    if (!booked.containsKey(r.id())) alert("not booked: " + r.id() + " " + ledger.show(r.receipt()).st());
```
무엇이 깨졌나: 벤더가 성공 신호(영수 번호·exit 0)를 기장 여부와 무관하게 냈는데, 호출부가 그 신호를 산출물 확인 대신 썼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 호출 전 64바이트 문자 경계 자르기 | 벤더 규칙(64B 초과 보류)을 안다 | 작음 | 메모 뒷부분 유실(62~64B만 남음) | 모든 post 호출부 — 기본 |
| 메모를 두 전표로 나눠 기장 | 금액 분할 허용 | 큼(대사 복잡) | 금액·건수 왜곡 | 선택하지 않은 방법 |
| 사후 `list`/`show` 대사 + 알림 | 조회 권한 | 중간(호출 수 증가) | 보류 자체는 막지 못함 | 자르기와 함께 안전망으로 |

**결론**: 자르기가 근본 대처이고, 대사는 규칙이 바뀌거나 다른 필드에서 같은 일이 생길 때를 위한 안전망이다. 둘을 함께 둔다.

## 검증 기록
- 2024-11-18: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
