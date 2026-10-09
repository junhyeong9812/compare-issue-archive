# issue/sanctlist/nfd-name-unmatched — 정답

태그: `encoding` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **일치하지 않는다.** 코드포인트 열이 다르다.

2. **라틴 문자는 NFC·NFD가 같다(악센트 없는 경우).**

3. **똑같이 보인다.** 렌더러가 자모를 조합해 그린다.

4. **명단 작성 기관이 7월부터 macOS 도구로 파일을 만들었다.** macOS 파일 처리 경로가 NFD를 쓴다. 7월 신규 국내 대상 5명 중 5명 모두 미탐.

5. **`sanctlist` 적재 단계에서 NFC로 정규화하고, 고객 이름도 저장 시 NFC로 맞춘다.** 적재 후 '한글 이름 중 NFC 아닌 것' 건수를 0으로 확인.

## 문제 구조 (추상화 코드)

### 변형 A — 정규화 없는 대조
① 문제 코드
```java
boolean hit = customer.name().equals(entry.name());
```
② 고친 코드
```java
String a = Normalizer.normalize(customer.name(), Normalizer.Form.NFC);
String b = Normalizer.normalize(entry.name(), Normalizer.Form.NFC);
boolean hit = a.equals(b);
```
무엇이 깨졌나: 같은 글자를 여러 바이트 열로 표현할 수 있는데, 대조가 바이트 동일성에 기댔다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 적재·저장 시 NFC | 적재 코드 | 작음 | 기존 고객 데이터 | 기본 |
| 기존 고객 이름 일괄 정규화 | 배치 | 중간 | 변환 잠금 | 함께 |
| 유사 매칭(자모 단위) | 엔진 기능 | 중간 | 오탐 증가 | 보조 |

**결론**: 외부 텍스트는 들어오는 순간 정규화한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
