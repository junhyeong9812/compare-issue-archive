# issue/connpx/new-db-user-default-pool-size — 정답

태그: `config` · `performance`

## 정답
<!-- 질문 1:1 대응 -->

1. **5개다.** 사용자별 설정은 이름으로 매칭되므로 `settle_rw`에는 적용되지 않고 `default_pool_size`가 쓰였다.

2. **프록시 대기열이다.** `SHOW POOLS`에서 `cl_waiting 25`, `maxwait 3`이 보였다.\
DB는 5개 커넥션만 받으니 한가했다.

3. **나아지지 않는다.** 앱이 더 많은 클라이언트 커넥션을 열어도 서버 커넥션은 5개라 대기만 는다.

4. **계정 교체 체크리스트**: `connpx.ini`의 사용자별 `pool_size` 이동, 배포 후 `SHOW POOLS`로 `sv_active` 상한 확인, 대시보드의 사용자 라벨 갱신.

## 문제 구조 (추상화 코드)

### 변형 A — 사용자별 풀 크기가 옛 계정 이름에만 있음
① 문제 코드
```ini
[users]
settle_app = pool_size=40
; settle_rw 없음 → default_pool_size=5
```
② 고친 코드
```ini
[users]
settle_rw = pool_size=40
```
무엇이 깨졌나: 풀 크기가 서비스가 아니라 계정 이름에 묶여 있다는 사실이 배포 절차에 없었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 사용자별 pool_size 추가 | 설정 반영 권한 | 작음 | 다음 계정 교체에서 반복 | 기본 |
| default_pool_size 상향 | DB 커넥션 여유 | 작음 | 작은 서비스도 커넥션 과다 | 선택하지 않은 방법 |
| 배포 후 SHOW POOLS 점검 자동화 | 프록시 관리 콘솔 접근 | 중간 | 점검 기준값 관리 | 재발 방지 |

**결론**: 풀 크기는 계정 설정과 함께 옮기고, 배포 후 `cl_waiting`을 확인하는 단계를 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
