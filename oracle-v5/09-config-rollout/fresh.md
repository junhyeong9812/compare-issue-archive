# fresh — 09-config-rollout (cfgsvc)

맞는 카드: `archive-v5-fresh/issue/cfgsvc/set-hidden-until-mark/`. 만든 방법: 3차 카드 본문 + 4차 낡은 카드의 버전·날짜 자리(버전만 현재 값으로 바꿈).
현재 규칙: cfgsvc 2.3.x. 마지막 mark 때 이미 값이 있던 키는 다음 mark 전까지 get이 이전 값을 준다(새 키는 바로 보임, keys st 3). 대처는 set 묶음 뒤, 읽기 전에 mark.

## 낡은 카드 대비 바꾼 줄 (diff 110줄 = - 56 / + 54)

- metadata.md 8: `벤더 버전` cfgsvc 1.8.x → 2.3.x
- 1-question.md 4~6: Q1 "오늘 아침 mark 뒤 이미 한 번 바꾼 키" → "이미 값이 있는 키", Q2·Q3 경계를 3차 문구(새 키 / 같은 키 두 번 / 신규 가맹점)로 되돌림
- 2-summary.md 5: 흐름 제목 버전 1.8.x → 2.3.x
- 2-summary.md 10~24 → 10~22: 흐름 그림의 보류 조건, mark 설명, 콘솔 예시, 올바른 교정(set 앞 → set 뒤 읽기 전), 발견 방법을 3차 문구로 되돌림
- 2-summary.md 29~33 → 27~31: 핵심 문장 5줄(규칙·새 키·mark·대처·검증)
- 3-answer.md 8, 10(버전 1.8.x → 2.3.x), 13~19, 21, 23, 26~27, 30~32: 정답 1~6의 규칙·대처 서술
- 3-answer.md 36~82: 변형 A~C 제목·주석·코드에서 mark 위치(set 앞 → set 뒤), "앞의 값" → "이전 값"
- 3-answer.md 88~90, 93: 방안 비교 표와 결론
- 그대로 둔 것: 사건·날짜(2024-06, 2024-06-12), 제목, 태그, 질문 4~6, 인덱스 README

## 3차 카드와 규칙 의미가 같다는 근거

- `diff -r archive-v3/issue/cfgsvc/set-hidden-until-mark archive-v5-fresh/issue/cfgsvc/set-hidden-until-mark`의 결과는 버전 표기 3곳(metadata 행, 2-summary 제목, 3-answer 1번)과 날짜 2곳(metadata 초안, 검증 기록), 사건 행의 "(2024-06)"뿐이다. 규칙 문장은 3차와 바이트 단위로 같다.
- 카드 대처("set 묶음 뒤, 읽기 전에 mark")는 fix.patch(20-apply 루프 뒤 `cfg_mark`)와 맞는다. fix.patch를 적용한 cases-v4/09에서 hidden이 통과했다(`# hidden: 0 failure(s)`).
