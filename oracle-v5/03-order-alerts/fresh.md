# 03-order-alerts — 맞는 카드 기록 (5차-a fresh)

카드: `archive-v5-fresh/issue/messaging/mqpub-batch-key-dedupe/` (출발점 = archive-v4-stale 같은 경로)
현재 규칙·버전: mqpub 3.4.1 — publish 1회(배치) 안에서 키의 앞 16바이트(짧으면 전체)가 같으면 첫 메시지만 저장, 나머지 무음 폐기(exit 0, accepted=요청 건수). 교정 = 전체 논리 키의 sha1 hex 16자를 키 맨 앞에.

## 낡은 카드 대비 바꾼 줄 (diff `<`·`>` 줄 합계 82)

- metadata.md: `벤더 버전` mqpub 2.6.2 → 3.4.1
- 1-question.md: 제목(끝 12 → 앞 16바이트), Q2(12 → 16바이트), Q4(공통 꼬리 `.<일자>`로 끝남 → 공통 접두로 시작), Q5(난수·해시를 키 끝 → 키 앞)
- 2-summary.md: "mqpub 2.6.x 기준" → "3.4.x 기준", 흐름도 키 3줄·비교 문자열("345.20240801" → "settle.done.M000")·정렬 공백, 규칙 비교 줄(끝 12 → 앞 16), 교정 키 형식(`<논리 키>:<해시 12자>` → `<해시 16자>:<논리 키>`)·설명, 핵심 문장 3줄(규칙·공통 접두·교정 위치)
- 3-answer.md: 답 1(비교 대상 `settle.done.M000`), 답 2(16바이트, 예 `abcd`), 답 4(`settle.done.` 12바이트 → 앞 4자리, 앞자리), 답 5(난수 접두·해시 접두 16자·식별자를 앞으로), 답 6(앞 16/17바이트째, 앞 15바이트 경계), 답 7(앞 16바이트 단언), 변형 A 제목·주석·고친 코드(`slice(0, 16)` 앞에), 무엇이 깨졌나, 검증 기록 버전(2.6.x → 3.4.x), 방안 비교 3행(난수 접두·식별자를 앞으로·해시 접두 64비트)·결론
- 그대로 둔 것: 경로, 사건(정산 완료 웹훅, 2024-08), 질문 1의 사건 키(`settle.done.M00012345.20240801`·`M00078345` — 현재 규칙에서도 앞 16바이트 `settle.done.M000`이 같아 1건), 날짜, 문장 구조

## 3차 카드와 규칙 의미가 같다는 근거

- `diff archive-v3/issue/messaging/mqpub-batch-key-head archive-v5-fresh/...`: 남는 차이는 제목 경로, 버전 줄 2개(2-summary·검증 기록), 사건 키·날짜(질문 1·흐름도 키 2줄), metadata 사건 행뿐 — 규칙·교정·방안 비교 줄은 3차와 글자 그대로 같다.
- 바이너리 확인: 질문 1 키 2줄을 한 배치로 publish → `accepted 2 batch 1` / `topic pz1 batches 1 messages 1`. fix.patch(3차 정답) 적용 후 hidden all passed.
