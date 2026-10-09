# issue/kvcache/resharding-ask-redirect-unhandled — 정답

태그: `client-compat` · `deploy`

## 정답
<!-- 질문 1:1 대응 -->

1. **`-ASK 5461 node-D`가 온다.** 키가 이미 옮겨졌다는 뜻이다.

2. **세션 서비스만 2018년판 클라이언트(ASK 미지원)를 쓰고 있었다.** 결제 서비스는 작년에 올렸다.

3. **끝나면 회복된다.** 슬롯 이전 완료 후 MOVED로 바뀌고 슬롯표가 갱신됐다 — 오류 구간이 이전 시간(20분)과 같았다.

4. **`kvcache clients --lib-version`으로 연결 클라이언트의 라이브러리 버전을 뽑아** ASK 지원 여부를 확인한다.

## 문제 구조 (추상화 코드)

### 변형 A — 구버전 클러스터 클라이언트
① 문제 코드
```xml
<dependency><artifactId>old-redis-cluster-client</artifactId><version>1.2.0</version></dependency>
```
② 고친 코드
```xml
<dependency><artifactId>lettuce-core</artifactId><version>6.3.2.RELEASE</version></dependency>
```
무엇이 깨졌나: 클러스터 운영 작업이 모든 클라이언트가 리다이렉트를 완전히 처리한다고 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 클라이언트 교체 | 코드 수정 | 중간 | 교체 중 회귀 | 기본 |
| 야간에 증설 | 트래픽 적음 | 작음 | 오류는 그대로 | 임시 |
| 증설 전 클라이언트 점검 | 목록 수집 | 작음 | 점검 누락 | 재발 방지 |

**결론**: 클라이언트를 교체하고, 증설 체크리스트에 클라이언트 버전 점검을 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
