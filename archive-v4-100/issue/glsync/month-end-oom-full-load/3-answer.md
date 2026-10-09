# issue/glsync/month-end-oom-full-load — 정답

태그: `performance` · `resource-exhaustion`

## 정답
<!-- 질문 1:1 대응 -->

1. **데이터량이 35배로 늘었는데 전량을 메모리에 올렸기 때문이다.**

2. **이번 달은 넘기지만 연말 결산에서 또 넘친다.** 메모리 사용이 데이터량에 비례하는 구조가 문제다.

3. **전표 번호 기준 체크포인트를 남기고 그 다음부터 읽는다.** ERP 쪽은 전표 번호로 멱등 처리한다.

## 문제 구조 (추상화 코드)

① 문제 코드
```java
List<JournalLine> all = repo.findByPeriod(period);   // 전량 로딩
erp.push(all);
```
② 고친 코드
```java
long after = checkpoint.load(period);
for (var page = repo.pageAfter(period, after, 5000); !page.isEmpty(); page = repo.pageAfter(period, after, 5000)) {
  erp.push(page); after = page.lastId(); checkpoint.save(period, after);
}
```
무엇이 깨졌나: 데이터량 상한이 없는 전량 로딩과 재시작 불가능한 구조.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 키셋 페이지 + 체크포인트 | 중간 | ERP 멱등 필요 | 기본 |
| 힙 증설 | 작음 | 다음 피크 | 임시 |

**결론**: 배치는 페이지 단위로 읽고, 중단 지점부터 이어 갈 수 있게 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
