# issue/lbctl/health-check-path-behind-auth — 정답

태그: `deploy` · `outage`

## 정답
<!-- 질문 1:1 대응 -->

1. **401을 받는다.** LB는 200이 아니면 비정상으로 본다.

2. **카나리 1대만 빠지고 트래픽은 나머지가 받았기 때문이다.** 카나리 지표(오류율)는 0이었다.

3. **503을 낸다.** 이 풀은 `panic_threshold` 설정이 없었다.

4. **인증 필터 제외 목록에 헬스 경로를 고정하고**, 카나리 단계에 `lbctl targets <pool>`의 healthy 확인을 넣는다.

## 문제 구조 (추상화 코드)

### 변형 A — 필터가 헬스 경로까지 덮음
① 문제 코드
```java
http.authorizeHttpRequests(a -> a.anyRequest().authenticated());
```
② 고친 코드
```java
http.authorizeHttpRequests(a -> a
        .requestMatchers("/internal/health").permitAll()
        .anyRequest().authenticated());
```
무엇이 깨졌나: 헬스체크 경로가 인프라 계약이라는 점이 앱 코드에 드러나 있지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 헬스 경로 permitAll + 테스트 | 앱 수정 | 작음 | 경로 이름 변경 | 기본 |
| 카나리 LB 상태 확인 | 배포 파이프라인 | 작음 | 없음 | 함께 |
| panic_threshold(fail-open) | LB 설정 | 작음 | 진짜 장애 노드에도 전송 | 보조 |

**결론**: 헬스 경로를 계약 테스트로 고정하고 카나리 검증에 LB 대상 상태를 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
