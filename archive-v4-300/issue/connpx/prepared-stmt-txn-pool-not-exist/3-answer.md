# issue/connpx/prepared-stmt-txn-pool-not-exist — 정답

태그: `environment-drift` · `config`

## 정답
<!-- 질문 1:1 대응 -->

1. **오류가 날 수 있다.** 6번째 실행의 EXECUTE가 `S_3`을 만든 백엔드가 아닌 곳으로 가면 `prepared statement "S_3" does not exist`가 난다.

2. **백엔드 커넥션 수 차이다.** 스테이징은 백엔드 4개, 운영은 60개였다 — 같은 백엔드로 다시 갈 확률이 운영에서 훨씬 낮다.\
또 스테이징 테스트는 짧아 임계값(5회)을 넘는 문장이 적었다.

3. **대부분 성공한다.** 오류는 실행 전 단계라 DB에 쓰기가 일어나지 않았다.\
다만 승인 서버는 이 오류를 5xx로 돌려 가맹점이 재요청했다 — 멱등 키가 있어 중복 승인은 없었다.

4. **드라이버 쪽이 기본이다.** `prepareThreshold=0`으로 끄면 프록시 버전과 무관하게 안전하다.\
`connpx` 1.10부터는 `track_prepared = on`이 있지만, 운영 버전 1.8에는 없었다.

## 문제 구조 (추상화 코드)

### 변형 A — 드라이버 기본값을 트랜잭션 풀링 뒤에서 그대로 씀
① 문제 코드
```properties
spring.datasource.url=jdbc:postgresql://connpx:6432/pay
# prepareThreshold 기본 5
```
② 고친 코드
```properties
spring.datasource.url=jdbc:postgresql://connpx:6432/pay?prepareThreshold=0
```
무엇이 깨졌나: 드라이버가 커넥션 하나를 계속 쓴다고 가정한 최적화가 프록시의 커넥션 교체와 맞지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| `prepareThreshold=0` | 드라이버 설정 변경 | 파싱 비용 소폭 증가 | 다른 서비스가 설정 누락 | 기본 |
| 프록시 업그레이드 + `track_prepared` | 1.10 이상 | 업그레이드 작업 | 업그레이드 전 기간 노출 | 장기 |
| 세션 풀링으로 회귀 | 커넥션 여유 | 커넥션 수 급증 | DB max_connections 초과 | 선택하지 않은 방법 |

**결론**: 드라이버에서 서버 측 prepare를 끄고, 프록시 업그레이드는 따로 계획한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
