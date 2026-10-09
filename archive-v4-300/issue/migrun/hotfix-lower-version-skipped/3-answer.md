# issue/migrun/hotfix-lower-version-skipped — 정답

태그: `silent-failure` · `environment-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **아무것도 적용하지 않고 exit 0이다.** `ignored` 줄이 INFO로만 찍힌다.

2. **스테이징은 V20250515가 아직 없었다.** 그래서 V20250512가 최신보다 높아 정상 적용됐다.

3. **ORM이 모르는 속성을 `extra` JSON에 넣는 설정이었다.** 사유 코드가 엉뚱한 곳에 저장되고 조회 화면만 비었다.

4. **`validate --pending-is-error`를 배포 파이프라인 마지막에 둔다.** 그리고 버전 번호는 머지 시점에 다시 매긴다.

## 문제 구조 (추상화 코드)

### 변형 A — 핫픽스 마이그레이션 번호를 작성 시점으로 매김
① 문제 코드
```sh
migrun apply --env prod      # V20250512 ignored, exit 0
```
② 고친 코드
```sh
migrun apply --env prod
migrun validate --env prod --pending-is-error || exit 1
```
무엇이 깨졌나: 마이그레이션이 '배포되면 적용된다'는 가정이 버전 순서 규칙 때문에 깨졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| validate로 실패 처리 | 파이프라인 수정 | 작음 | 이미 순서 꼬인 이력 정리 필요 | 기본 |
| out_of_order=true | 마이그레이션 간 독립 | 작음 | 의존 순서 꼬임 | 선택하지 않은 방법 |
| 머지 시 번호 재부여 | 머지 봇 | 중간 | 수동 머지 시 누락 | 보조 |

**결론**: 미적용을 실패로 올리고, 번호는 머지 순서로 매긴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
