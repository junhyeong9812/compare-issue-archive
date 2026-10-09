# issue/kdeploy/reused-tag-ifnotpresent-mixed-image — 정답

태그: `environment-drift` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **옛 코드다.** 노드 1~7의 파드가 반올림 버그를 그대로 가졌다.

2. **태그는 같고 digest만 달랐기 때문이다.** 배포 기록은 태그만 남긴다.

3. **모든 노드가 새로 받아 같은 코드가 됐을 것이다.** 하지만 태그 재사용 자체가 문제다.

4. **정산 결과에 처리 파드 이름을 남겨 두었다.** 차액 건을 파드별로 묶으니 7개 노드에 몰려 있었고, `imageID`가 둘로 나뉘었다.

## 문제 구조 (추상화 코드)

### 변형 A — 태그를 덮어써 핫픽스
① 문제 코드
```sh
docker push registry/settle:2.8.1   # 같은 태그 재푸시
kdeploy rollout settle --tag 2.8.1
```
② 고친 코드
```sh
docker push registry/settle:2.8.2
kdeploy rollout settle --image registry/settle@sha256:9f1c...
```
무엇이 깨졌나: 이미지 식별을 가변 이름으로 했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| digest 고정 배포 | 배포 도구 지원 | 작음 | 사람이 읽기 어려움 | 기본 |
| 레지스트리 태그 immutable | 레지스트리 설정 | 작음 | 급한 핫픽스 불편 | 함께 |
| pullPolicy Always | 레지스트리 가용성 | 기동 지연 | 레지스트리 장애 시 기동 불가 | 보조 |

**결론**: 태그 재사용을 금지하고 digest로 배포한다. 차이 기간 정산은 재계산해 보정했다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
