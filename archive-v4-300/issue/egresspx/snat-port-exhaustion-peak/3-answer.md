# issue/egresspx/snat-port-exhaustion-peak — 정답

태그: `performance` · `network`

## 정답
<!-- 질문 1:1 대응 -->

1. **약 930건/초.**

2. **PG가 `Connection: close`를 보내 풀이 매번 닫았다.**

3. **IP 수에 비례해 상한이 오른다.** 임시로 4개로 늘렸다.

4. **PG와 keep-alive 협의 후 재사용.** 포트 사용률 70% 경보.

## 문제 구조 (추상화 코드)

### 변형 A — 연결을 요청마다 새로 엶
① 문제 코드
```java
HttpRequest req = HttpRequest.newBuilder(uri).header("Connection", "close").build();
```
② 고친 코드
```java
HttpRequest req = HttpRequest.newBuilder(uri).build();   // keep-alive, 공유 HttpClient
```
무엇이 깨졌나: 연결 비용을 앱 안에서만 보고 중간 장비의 자원을 몰랐다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 연결 재사용 | PG 협조 | 작음 | PG 측 제한 | 기본 |
| 출발 IP 증설 | IP 등록(PG 허용 목록) | 중간 | 상한 이동일 뿐 | 임시 |
| 포트 사용률 경보 | 메트릭 | 작음 | 없음 | 함께 |

**결론**: 연결을 재사용하고 출발 포트 사용률을 감시한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
