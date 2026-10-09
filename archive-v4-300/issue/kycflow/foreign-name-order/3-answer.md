# issue/kycflow/foreign-name-order — 정답

태그: `contract-drift` · `identity`

## 정답
<!-- 질문 1:1 대응 -->

1. **불일치다.** 공백을 빼도 `VANANNGUYEN`과 `NGUYENVANAN`이 달라 자동 거절된다.

2. **2.x는 판독 영역 순서 그대로 성+이름으로 결합했다.** 3.0은 '표시 형식 국제화'로 서구식 순서를 기본으로 바꿨다.

3. **없었다.** 한국인은 신분증 OCR 경로를 탄다. 그래서 전체 거절률은 0.8%p만 올랐고, 외국인 세그먼트만 3%→41%였다.

4. **불일치를 '실명 불일치 = 거절'로 바로 처리하는 규칙이었기 때문이다.** 2월 4~6일 외국인 고객 318명이 거절 안내를 받았다.

5. **성·이름을 따로 정규화(대문자·공백·하이픈 제거)해 성끼리, 이름끼리 비교**한다. 응답이 결합형뿐이면 두 순서를 모두 시도하고 결과를 사람 검토로 보낸다.

## 문제 구조 (추상화 코드)

### 변형 A — 표시용 이름 문자열로 비교
① 문제 코드
```java
String ours = norm(passport.displayName());     // 3.0: "VAN AN NGUYEN"
return ours.equals(norm(bank.realName()));       // "NGUYENVANAN"
```
② 고친 코드
```java
String sg = norm(passport.surname()) + norm(passport.givenNames());
String gs = norm(passport.givenNames()) + norm(passport.surname());
String bank = norm(bank.realName());
if (bank.equals(sg)) return MATCH;
return bank.equals(gs) ? MATCH_REVIEW : MISMATCH_REVIEW;   // 자동 거절 없음
```
무엇이 깨졌나: 표시용 형식을 비교 키로 썼고, 벤더가 표시 형식을 바꾸자 비교가 전부 깨졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 구조화 칸 비교 | 판독 결과에 성·이름 칸 | 작음 | 응답이 결합형 | 기본 |
| 두 순서 시도 + 검토 | 검토 인력 | 중간 | 검토 부담 | 결합형 응답 |
| 거절률 세그먼트 경보 | 지표 | 작음 | 사후 | 안전망 |

**결론**: 이름 비교는 표시 문자열에 기대지 않는다. 벤더 업그레이드 후 세그먼트별 거절률을 본다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
