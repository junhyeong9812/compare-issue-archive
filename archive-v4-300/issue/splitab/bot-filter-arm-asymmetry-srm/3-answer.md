# issue/splitab/bot-filter-arm-asymmetry-srm — 정답

태그: `data-quality` · `statistics` · `experimentation`

## 정답
<!-- 질문 1:1 대응 -->

1. **표본 비율 불일치(SRM)부터 의심하고 판독을 멈춘다.**\
배정 로그와 노출 로그의 비율을 단계별로 비교한다.

2. **노출 집계의 봇 필터가 B안 새 랜딩 경로에만 걸려 정상 사용자 일부를 제외했다.**

3. **빠진 사용자가 특정 기기에 몰려 두 군이 같은 모집단이 아니게 된다.**\
전환율 차이가 처치 효과인지 필터 효과인지 가를 수 없다.

4. **필터를 경로와 무관하게 양쪽 동일 적용, 분모는 배정 로그 기준, 필터 규칙 변경 시 진행 중 테스트 목록 확인.**\
이번 테스트는 기간을 새로 잡아 다시 돌렸다.

## 문제 구조 (추상화 코드)

### 변형 A — 군마다 다른 집계 필터
① 문제 코드
```sql
SELECT arm, COUNT(DISTINCT user_id) FROM exposures e
JOIN bot_filter_by_path f ON e.path = f.path AND NOT f.is_bot(e.ua) GROUP BY arm;
```
② 고친 코드
```sql
SELECT arm, COUNT(DISTINCT user_id) FROM assignments a
WHERE NOT is_bot_user(a.user_id)          -- 경로와 무관한 사용자 단위 필터
GROUP BY arm;
```
무엇이 깨졌나: 측정 단계의 처리가 군마다 달라 배정의 무작위성이 깨졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 사용자 단위 동일 필터 | 필터 재설계 | 중간 | 봇 일부 잔존 | 기본 |
| 배정 로그 분모 | 배정 로그 보존 | 작음 | 미노출 사용자 포함 해석 | 기본 |
| 불일치 보정 가중치 | 통계 처리 | 중간 | 편향 남음 | 선택하지 않은 방법 |

**결론**: SRM은 판독 중단 신호다 — 군 사이 처리 차이를 먼저 찾는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
