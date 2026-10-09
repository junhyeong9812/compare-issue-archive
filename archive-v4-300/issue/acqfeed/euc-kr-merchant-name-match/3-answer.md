# issue/acqfeed/euc-kr-merchant-name-match — 정답

태그: `encoding` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **대체 문자가 섞인 문자열이 넘어간다.** 바이트열이 UTF-8로 해석되지 않아 `�` 연속이 되고, 파서는 이를 정상 필드로 넘긴다. exit 0, 경고 없음.

2. **매칭이 가맹점번호 우선이기 때문이다.** 단독 가맹점은 번호만으로 맞는다. 프랜차이즈 하위 지점 37곳은 대표 번호를 공유해 이름으로 갈라야 했고, 이름이 깨져 전부 '매칭 실패'로 떨어졌다.

3. **받지 않는다.** ASCII 범위는 두 인코딩에서 같은 바이트다. 그래서 영문 상호 가맹점에서는 재현되지 않아 원인 파악이 늦었다.

4. **매칭 키 필드를 원본 바이트로 확인한다.** `acqfeed dump --raw <line>`으로 원문 바이트를 보고, 사내 값과 키 단위로 비교한다. 금액이 같으면 키 해석 쪽이다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
String name = new String(bytes, 120, 40, StandardCharsets.UTF_8).trim();
```
② 고친 코드
```java
Charset cs = cardProfile(cardCode).charset();          // HN = MS949
CharsetDecoder dec = cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
String name = dec.decode(ByteBuffer.wrap(bytes, 120, 40)).toString().trim();
```
무엇이 깨졌나: 전역 인코딩 하나를 모든 상대에 가정했고, 디코딩 실패를 대체 문자로 삼켰다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 카드사별 인코딩 + REPORT 모드 | 설정 1줄씩 | 새 카드사 설정 누락 시 파싱 실패(보이는 실패) | 기본 |
| 하위 가맹점 매칭을 사업자번호로 변경 | 매핑 테이블 정비 | 사업자번호 공유 지점은 여전히 모호 | 보조 |

**결론**: 인코딩은 상대별 계약값으로 두고, 깨진 디코딩은 실패로 올린다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
