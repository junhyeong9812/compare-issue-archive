# issue/bkvault/gfs-monthly-picks-failed-weekly — 정답

태그: `backup` · `retention`

## 정답
<!-- 질문 1:1 대응 -->

1. **없다.** 승격할 주간본이 없었다.

2. **수동 실행은 일간 백업으로 기록돼 승격 규칙에 맞지 않았다.**

3. **`promote_fallback: false`였다.** 규칙이 첫 일요일만 봤다.

4. **`promote_fallback: first_success`**로 바꾸고, 수동 재실행에 `--tag weekly`를 쓰게 런북을 고쳤다.

## 문제 구조 (추상화 코드)

### 변형 A — 승격이 고정 슬롯에만 의존
① 문제 코드
```yaml
retention: { daily: 14, weekly: 8, monthly: 12, monthly_from: first_sunday }
```
② 고친 코드
```yaml
retention: { daily: 14, weekly: 8, monthly: 12, monthly_from: first_sunday,
             promote_fallback: first_success }
```
무엇이 깨졌나: 보관 정책이 백업 성공을 전제로 짜여 있었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| fallback 승격 | 설정 | 작음 | 월간본 시점이 며칠 밀림 | 기본 |
| 월간 별도 작업 | 작업 추가 | 중간 | 저장 비용 | 규제 요건 클 때 |
| 월간본 존재 점검 | 점검 스크립트 | 작음 | 발견 지연 | 함께 |

**결론**: 승격 대체 규칙을 켜고, 월초에 직전 달 보관본 존재를 점검한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
