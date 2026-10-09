# issue/cipipe/shallow-clone-version-unknown-overwrite — 정답

태그: `ci` · `deploy`

## 정답
<!-- 질문 1:1 대응 -->

1. **`describe` 실패 → `0.0.0-unknown`.**

2. **두 빌드가 같은 버전 문자열을 썼고 저장소가 덮어쓰기를 허용했다.**

3. **스크립트가 실패를 기본값으로 바꿨다.**

4. **`git fetch --tags --depth=200` 후 계산, 실패하면 exit 1.** 저장소는 릴리스 버전 덮어쓰기 금지.

## 문제 구조 (추상화 코드)

### 변형 A — 버전 계산 실패를 기본값으로 대체
① 문제 코드
```sh
VERSION=$(git describe --tags 2>/dev/null || echo 0.0.0-unknown)
```
② 고친 코드
```sh
git fetch --tags --depth=200 origin
VERSION=$(git describe --tags) || { echo "version 계산 실패" >&2; exit 1; }
```
무엇이 깨졌나: 실패를 그럴듯한 값으로 바꿔 다음 단계로 넘겼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 태그 fetch + 실패 시 중단 | 스크립트 수정 | 작음 | fetch 시간 | 기본 |
| 버전을 CI 변수로 주입 | 파이프라인 | 작음 | 수동 실수 | 보조 |
| 저장소 덮어쓰기 금지 | 저장소 설정 | 작음 | 재업로드 불편 | 함께 |

**결론**: 버전 계산 실패는 빌드 실패로 만들고 저장소는 덮어쓰기를 막는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
