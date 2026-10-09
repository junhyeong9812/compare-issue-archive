# issue/batchd/container-utc-file-date — 정답

태그: `environment-drift` · `timezone`

## 정답
<!-- 질문 1:1 대응 -->

1. **전날 날짜다.** UTC로는 전날 18:00이다.

2. **09:00 KST 이후는 UTC로도 같은 날짜다.**

3. **당장은 고쳐지지만 다른 실행 환경에서 다시 깨진다.** 업무 날짜를 파라미터로 받는 것이 근본 대처다.

## 문제 구조 (추상화 코드)

### 변형 A — 시스템 시간대에 기댄 날짜
① 문제 코드
```java
String file = "settle_" + LocalDate.now().format(BASIC_ISO_DATE) + ".dat";
```
② 고친 코드
```java
LocalDate bizDate = params.bizDate()                                  // 스케줄러가 넘김
        .orElse(LocalDate.now(ZoneId.of("Asia/Seoul")));
String file = "settle_" + bizDate.format(BASIC_ISO_DATE) + ".dat";
```
무엇이 깨졌나: 업무 날짜를 실행 환경의 시간대라는 암묵적 입력에 맡겼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 업무 날짜 파라미터화 | 스케줄러 지원 | 중간 | 없음 | 기본 |
| 이미지에 TZ 고정 | 베이스 이미지 관리 | 작음 | 다른 환경 재발 | 함께 적용 |

**결론**: 업무 날짜는 명시 입력으로 받고, 이관 체크리스트에 시간대를 넣는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
