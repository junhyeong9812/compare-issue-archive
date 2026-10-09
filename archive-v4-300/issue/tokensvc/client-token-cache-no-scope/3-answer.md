# issue/tokensvc/client-token-cache-no-scope — 정답

태그: `caching` · `authorization`

## 정답
<!-- 질문 1:1 대응 -->

1. **403이다.** read 토큰이 재사용된다.

2. **재시작 후 첫 호출이 무엇이냐에 따라 캐시된 scope가 달라졌다.**

3. **한 토큰으로 둘 다 된다.** 다만 최소 권한 원칙상 조회 경로에 지급 권한을 싣지 않는다.

4. **지급 요청은 재시도 큐로 들어가 몇 시간 뒤 토큰 만료·재발급 때 성공했다.** 지급 지연 문의로 발견.

5. **`client_id + 정렬된 scope 목록`.** 403 응답 시 해당 캐시를 지우고 1회 재발급.

## 문제 구조 (추상화 코드)

### 변형 A — client_id 캐시
① 문제 코드
```java
Token t = cache.computeIfAbsent(clientId, id -> issue(id, scope));
```
② 고친 코드
```java
String key = clientId + "|" + String.join(" ", new TreeSet<>(scopes));
Token t = cache.computeIfAbsent(key, k -> issue(clientId, scopes));
```
무엇이 깨졌나: 캐시 키가 값을 결정하는 입력(scope)을 다 담지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 키에 scope 포함 | 코드 수정 | 작음 | 없음 | 기본 |
| 403 시 캐시 무효화 | 코드 수정 | 작음 | 반복 403 | 함께 |
| 한 토큰에 전체 scope | 파트너 설정 | 작음 | 권한 과다 | 선택하지 않은 방법 |

**결론**: 캐시 키는 결과를 바꾸는 모든 입력을 포함한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
