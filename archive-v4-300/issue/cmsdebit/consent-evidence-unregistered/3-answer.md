# issue/cmsdebit/consent-evidence-unregistered — 정답

태그: `async-status` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **접수는 성공하고 결과 파일에서 '증빙 미제출' 불능이 된다.**

2. **가입 화면 개편에서 증빙 업로드 호출이 빠졌다.** 기존 회원은 증빙이 이미 있다.

3. **재신청하면 된다.** 대행사 처리 일정상 다음 출금 회차에 포함된다.

4. **잔액 부족·계좌 해지·증빙 미제출을 구분해 집계한다.** 증빙 미제출이 늘면 우리 쪽 결함이다.

## 문제 구조 (추상화 코드)

① 문제 코드
```python
cms.member_add(m); signup.complete(m)
```
② 고친 코드
```python
cms.member_add(m)
cms.evidence_put(m.id, signature_file)
assert cms.evidence_status(m.id) == 'REGISTERED'
signup.complete(m)
```
무엇이 깨졌나: 2단계 등록의 두 번째 단계가 빠져도 첫 단계가 성공으로 보였다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 증빙 확인 후 출금 대상 편입 | 작음 | 없음 | 기본 |
| 불능 후 재신청 | 반복 | 회원 연체 처리 | 선택하지 않음 |

**결론**: 출금 대상은 증빙 등록 확인으로 판정한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
