# issue/evstream/schema-compat-none-type-widen — 정답

태그: `deploy` · `contract`

## 정답
<!-- 질문 1:1 대응 -->

1. **역직렬화 실패다.** long → int는 허용되지 않는 변환이다.

2. **CI는 레지스트리의 subject 설정을 따랐고, 설정이 NONE이었다.**

3. **거절됐다.** 그래서 소비자 선배포 순서가 강제된다.

## 문제 구조 (추상화 코드)

### 변형 A — 호환성 검사 꺼진 상태로 타입 변경
① 문제 코드
```sh
registry config pay.completed-value --compat NONE
producer deploy v8
```
② 고친 코드
```sh
registry config pay.completed-value --compat BACKWARD
registry check pay.completed-value schema-v8.avsc || exit 1
deploy consumers --schema v8 && producer deploy v8
```
무엇이 깨졌나: 긴급 조치로 끈 안전장치를 복구하지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| BACKWARD + 소비자 선배포 | 배포 순서 관리 | 작음 | 순서 누락 | 기본 |
| 새 필드 추가 후 옛 필드 폐기 | 이중 필드 기간 | 중간 | 정리 누락 | 타입 변경 |

**결론**: 호환성 설정은 레지스트리 감사 대상으로 두고, 타입 변경은 새 필드 추가 방식으로 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
