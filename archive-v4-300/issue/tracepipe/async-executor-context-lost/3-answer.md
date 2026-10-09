# issue/tracepipe/async-executor-context-lost — 정답

태그: `observability` · `context`

## 정답
<!-- 질문 1:1 대응 -->

1. **없다 — 새 루트 트레이스가 된다.**

2. **@Async는 에이전트가 계측한 스프링 실행기를 썼다.**

3. **끊긴다.** 로그에서도 요청 ID가 비었다.

4. **풀 생성 지점 한 곳.** 래핑 실행기와 MDC 복사 데코레이터를 넣었다.

## 문제 구조 (추상화 코드)

### 변형 A — 직접 만든 풀에 작업 위임
① 문제 코드
```java
ExecutorService refundPool = Executors.newFixedThreadPool(16);
```
② 고친 코드
```java
ExecutorService refundPool =
    Context.taskWrapping(Executors.newFixedThreadPool(16));   // 컨텍스트 전파
```
무엇이 깨졌나: 관측 컨텍스트가 스레드 경계를 자동으로 넘는다고 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 실행기 래핑 | 코드 수정 | 작음 | 새 풀 추가 시 누락 | 기본 |
| 공용 실행기 팩토리 | 규약 | 작음 | 규약 미준수 | 함께 |
| 에이전트 설정 확장 | 에이전트 버전 | 작음 | 모든 풀 대응 불가 | 보조 |

**결론**: 풀 생성을 팩토리 하나로 모으고 그곳에서 래핑한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
