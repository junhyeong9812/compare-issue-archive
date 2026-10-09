# issue/unsubreg/consent-date-string-compare — 정답

태그: `compliance` · `data-type`

## 정답
<!-- 질문 1:1 대응 -->

1. **아니다.** 문자열 비교에서 "2021-3"이 "2021-0"보다 크다.

2. **신규 데이터는 자릿수가 맞춰진 형식이었다.**

3. **27건이 "2021/3/7" 등 다른 구분자였다.** 수기 확인 후 이관했다.

## 문제 구조 (추상화 코드)

### 변형 A — 문자열 날짜 비교
① 문제 코드
```sql
SELECT phone FROM consent WHERE consent_date <= :cutoff_str;
```
② 고친 코드
```sql
ALTER TABLE consent ADD COLUMN consent_on DATE;
UPDATE consent SET consent_on = to_date(consent_date, 'YYYY-FMMM-FMDD');
SELECT phone FROM consent WHERE consent_on <= current_date - interval '2 years';
```
무엇이 깨졌나: 날짜를 문자열 타입으로 저장해 비교 의미가 달라졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| DATE 타입 이관 | 이관 작업 | 중간 | 파싱 실패 행 | 기본 |
| 조회 시 변환 | 없음 | 매번 비용 | 변환 실패 무시 | 임시 조치 |

**결론**: 날짜는 날짜 타입으로 저장하고, 이관 시 형식 혼재를 먼저 센다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
