# issue/cipipe/allow-failure-integration-stage — 정답

태그: `ci` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **둘 다 통과로 본다.** 경고 아이콘만 뜬다.

2. **2022년 말 PG 샌드박스 장애 때 '일주일만' 넣었다.**

3. **항상 떠 있어 신호가 아니었다.**

4. **테스트 단위로 격리 목록을 두고 기한을 붙인다.** 단계 전체를 허용 실패로 두지 않는다.

## 문제 구조 (추상화 코드)

### 변형 A — 단계 전체 실패 허용
① 문제 코드
```yaml
integration:
  script: ./gradlew integrationTest
  allow_failure: true
```
② 고친 코드
```yaml
integration:
  script: ./gradlew integrationTest -PexcludeTags=quarantine
# quarantine 태그: 테스트별, 만료일 주석 필수
```
무엇이 깨졌나: 불안정한 외부 의존 때문에 회귀 감지 전체를 꺼 버렸다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 테스트별 격리 + 만료 | 태그 관리 | 작음 | 격리 남용 | 기본 |
| 샌드박스 대신 계약 테스트 | 목 서버 | 중간 | 실제와 차이 | 장기 |
| allow_failure 유지 | 없음 | 없음 | 회귀 통과 | 선택하지 않은 방법 |

**결론**: 허용 실패를 없애고 격리를 테스트 단위·기한제로 바꾼다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
