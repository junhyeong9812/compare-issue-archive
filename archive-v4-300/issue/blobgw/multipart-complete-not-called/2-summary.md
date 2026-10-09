# issue/blobgw/multipart-complete-not-called — 정리 (힌트)

태그: `silent-failure` · `partial-failure`

## 전체 흐름

```
initiate → upload part 1..30 (각 200) → [파드 축출] → complete 미호출
버킷 목록: 객체 없음
미완료 업로드: list-multipart-uploads 에만 보임 (스토리지 과금 계속)
작업 기록: 'part 30/30' 로그 시점에 성공 처리
```

## 핵심 문장

- 멀티파트는 **`complete`가 성공해야** 객체가 생긴다 — 파트 업로드 성공은 중간 단계다.
- 업로더가 성공 기록을 `complete` 전에 남겨 실패가 숨었다.
- 미완료 업로드는 목록에 안 보이지만 파트 저장 비용은 계속 든다 — 수명 주기 규칙으로 정리한다.
- 성공 판정은 `complete` 응답의 ETag와 `head` 조회로 한다.
