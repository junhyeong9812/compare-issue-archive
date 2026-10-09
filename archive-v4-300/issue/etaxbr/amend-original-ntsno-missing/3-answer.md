# issue/etaxbr/amend-original-ntsno-missing — 정답

태그: `contract-drift` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **중계사 조회가 실패하고 국세청 전송에서 반려된다.** 요청 응답은 ACCEPTED라 그 자리에서는 모른다.

2. **반려 87건은 모두 옛 중계사로 발행된 당초분이었다.**

3. **동작은 하지만 쓰지 않는다.** 중계사를 또 바꾸면 같은 일이 난다. 정식 값(승인번호)으로 통일했다.

4. **당초분 발행 중계사·승인번호 존재 여부를 건별로 본다.** 수정발행 결과(nts)를 당초분과 짝지어 대사한다.

## 문제 구조 (추상화 코드)

① 문제 코드
```python
etax.amend(orig=inv.doc_no, reason='환입', items=diff)
```
② 고친 코드
```python
nts_no = inv.nts_approval_no or legacy_lookup(inv)      # 24자리 승인번호
if not nts_no: raise MissingOriginalApproval(inv.doc_no)
etax.amend(orig=nts_no, reason='환입', items=diff)
```
무엇이 깨졌나: 벤더 편의 기능(사내 키 해석)에 기대 정식 식별자 저장을 미뤘다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 승인번호 저장 + 옛 건 백필 | 백필 작업 | 백필 누락 건 | 기본 |
| 옛 중계사 건은 수기 처리 | 반복 수작업 | 누락 | 선택하지 않음 |

**결론**: 외부 기관 식별자는 발행 시점에 저장하고, 수정 요청은 그 값으로 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
