# issue/cdcpipe/toast-unchanged-placeholder-overwrite — 정답

태그: `data-semantics` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **설명이 TOAST에 저장된 상품이면 자리표시 문자열이다.** 짧은 설명은 실제 값이 실린다.

2. **INSERT는 모든 컬럼 값을 싣는다.** 자리표시는 UPDATE에서 그 컬럼이 안 바뀐 경우에만 생긴다.

3. **옛 행 전체가 WAL에 실려 해결되지만 WAL 양이 크게 는다.** 상품 테이블에서 약 3배로 측정돼 보류했다.

4. **해당 이벤트 원문에서 필드 값이 자리표시인지 본다.** 자리표시면 이 사건, 실제 값이면 인덱서 변환 쪽이다.

## 문제 구조 (추상화 코드)

### 변형 A — 이벤트 값으로 문서 전체 교체
① 문제 코드
```java
index.put(evt.after().get("id"), evt.after());
```
② 고친 코드
```java
Map<String,Object> doc = new HashMap<>(evt.after());
doc.values().removeIf(v -> UNAVAILABLE.equals(v));     // 미변경 큰 컬럼은 갱신하지 않음
index.partialUpdate(evt.after().get("id"), doc);
```
무엇이 깨졌나: 이벤트가 "행 전체의 현재 값"이라고 가정했다 — 실제로는 큰 미변경 컬럼이 빠진 값이었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 자리표시 제외 부분 갱신 | 인덱스 부분 갱신 지원 | 작음 | 없음 | 기본 |
| REPLICA IDENTITY FULL | WAL 여유 | WAL 3배 | 디스크·복제 지연 | 작은 테이블 |

**결론**: CDC 이벤트의 자리표시 값은 갱신에서 빼고, 문서 갱신은 부분 갱신으로 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
