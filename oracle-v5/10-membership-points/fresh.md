# fresh — 10-membership-points (ptsctl)

맞는 카드: `archive-v5-fresh/issue/ptsctl/earn-member-leading-zero/`. 만든 방법: 3차 카드 본문 + 4차 낡은 카드의 버전·날짜 자리(버전만 현재 값으로 바꿈).
현재 규칙: ptsctl 4.2.x. earn은 숫자로만 된 회원번호를 길이와 무관하게 수로 읽어 앞자리 0을 버린다. 대처는 모든 earn에 `@` 표기.

## 낡은 카드 대비 바꾼 줄 (diff 52줄 = - 28 / + 24)

- metadata.md 8: `벤더 버전` ptsctl 3.6.x → 4.2.x
- 1-question.md 5~6: Q2에서 "`0000417`처럼 6자리가 아닌 숫자일 때" 제거, Q3에서 "법인 회원" 제거(3차 문구)
- 2-summary.md 5: 흐름 제목 버전 3.6.x → 4.2.x
- 2-summary.md 9, 19~21, 27~28: 해석 대상 "숫자 6자리" → "숫자로만 됨", 0000417 예시 줄 제거, 교정 "숫자 6자리만 @" → "earn --mbr @ + 발급 번호"
- 2-summary.md 34, 37: 핵심 문장(규칙·교정)
- 3-answer.md 13~15(13에 버전 3.6.x → 4.2.x), 20, 24, 33: 정답 2·3·4·6의 규칙·대처 서술
- 3-answer.md 46~49, 51, 62, 73: 변형 A~C 코드에서 `[0-9]{6}` 분기 → 모든 번호 `"@" +`
- 3-answer.md 82~83, 87: 방안 비교 표와 결론
- 그대로 둔 것: 사건·날짜(2024-08, 2024-09-03), 제목, 태그, 질문 1·4~6, "카드에 찍힌 6자리(앞 0 포함)"(매장 카드 형식 설명으로 3차·4차에 같은 줄), 인덱스 README

## 3차 카드와 규칙 의미가 같다는 근거

- `diff -r archive-v3/issue/ptsctl/earn-member-leading-zero archive-v5-fresh/issue/ptsctl/earn-member-leading-zero`의 결과는 버전 표기 3곳(metadata 행, 2-summary 제목, 3-answer 2번)과 날짜 2곳(metadata 초안, 검증 기록), 사건 행의 "2024-08, "뿐이다. 규칙 문장은 3차와 바이트 단위로 같다.
- 카드 대처("모든 earn에 @ + 발급 번호")는 fix.patch(`mbr()` = `"@" + memberNo`, earn/bal/hist 공통)와 맞는다. fix.patch를 적용한 cases-v4/10에서 hidden이 통과했다(`hidden: 3 passed, 0 failed`).
