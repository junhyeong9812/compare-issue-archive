# issue/srchidx/dynamic-false-new-field-unsearchable — 매핑이 dynamic:false면 새 필드는 저장만 되고 검색 대상이 아니다 — 질문

## 질문
1. (예측) `merchants` 인덱스 매핑은 `dynamic: false`다. 색인기가 새 필드 `biz_type`을 문서에 넣어 보내면, 문서 조회(`_source`)와 `term` 검색은 각각 어떻게 되는가?
2. (왜) 색인 응답은 모두 `result: created`였다. 왜 아무 신호가 없었나?
3. (경계) 매핑에 필드를 추가한 뒤 바로 검색되는가?
4. (연결) 필드 추가 배포 순서는 어떻게 잡아야 하나?

## 복습 기록
| 날짜 | 결과 | 틀린 질문 | 다음 복습 |
|------|------|-----------|-----------|
| | | | |
