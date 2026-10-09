# issue/srchidx/dynamic-false-new-field-unsearchable — 정답

태그: `contract-drift` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **`_source`에는 보이고 `term` 검색은 0건이다.**

2. **`dynamic: false`에서는 모르는 필드가 정상 처리이기 때문이다.** 오류도 경고도 없다.

3. **추가 이후 색인된 문서만 검색된다.** 기존 9만 건은 `_update_by_query`로 다시 색인했다.

4. **매핑 먼저, 색인기 나중, 재색인 마지막.** 인덱스는 `dynamic: strict`로 바꿔 매핑 누락을 색인 오류로 드러냈다.

## 문제 구조 (추상화 코드)

### 변형 A — 매핑 없이 필드만 추가
① 문제 코드
```json
PUT merchants/_doc/m-1 { "name": "...", "biz_type": "CORP" }
```
② 고친 코드
```json
PUT merchants/_mapping { "properties": { "biz_type": { "type": "keyword" } } }
POST merchants/_update_by_query?conflicts=proceed
```
무엇이 깨졌나: 필드 추가가 색인기 코드만의 변경이라고 보고 인덱스 계약을 바꾸지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 매핑 선반영 + 재색인 | 매핑 관리 권한 | 작음 | 재색인 중 부하 | 기본 |
| dynamic:strict | 모든 필드 명시 | 작음 | 매핑 누락 시 색인 실패 | 함께 |
| dynamic:true | 필드 폭증 허용 | 작음 | 타입 오추론·필드 폭증 | 선택하지 않은 방법 |

**결론**: 매핑을 계약으로 관리하고 strict로 누락을 오류로 만든다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
