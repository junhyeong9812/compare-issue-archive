# issue/sagaorc/lock-order-inversion-deadlock — 정답

태그: `concurrency` · `performance`

## 정답
<!-- 질문 1:1 대응 -->

1. **교착이다.** DB가 한쪽을 롤백하고 사가가 재시도하며 반복된다.

2. **부하 테스트에 결제 실패 비율이 0이었다.** 보상 경로가 돌지 않았다.

3. **교착은 시간으로 풀리지 않는다.** 순서를 고정해야 한다.

## 문제 구조 (추상화 코드)

### 변형 A — 경로별로 다른 잠금 순서
① 문제 코드
```java
// 보상
pointRepo.lockForUpdate(memberId); stockRepo.lockForUpdate(sku);
```
② 고친 코드
```java
// 모든 경로 공통 순서: 재고 → 포인트
stockRepo.lockForUpdate(sku); pointRepo.lockForUpdate(memberId);
```
무엇이 깨졌나: 잠금 순서를 경로마다 따로 정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 전역 잠금 순서 고정 | 순서 규약 | 작음 | 새 경로 위반 | 기본 |
| 낙관적 잠금 | 버전 컬럼 | 중간 | 경합 시 재시도 증가 | 경합 낮은 자원 |

**결론**: 여러 자원을 잠그는 모든 경로는 같은 순서를 쓰고, 부하 테스트에 실패 경로를 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
