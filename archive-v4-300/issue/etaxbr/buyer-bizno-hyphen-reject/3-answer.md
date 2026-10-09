# issue/etaxbr/buyer-bizno-hyphen-reject — 정답

태그: `silent-failure` · `async-status`

## 정답
<!-- 질문 1:1 대응 -->

1. **ISSUED를 받고, 다음 날 국세청 전송에서 NTS_REJECTED가 된다.** 거래처는 이메일은 받았지만 홈택스에는 없다.

2. **거래처 마스터 이관 작업이 그 기간 데이터만 하이픈 포함 형식으로 넣었다.** 이전 데이터는 숫자 10자리였다.

3. **같은 공급 건으로 두 번 발행한 기록이 남아 수정발행 대상이 된다.** 반려 건은 `etaxbr resend <id>`로 고친 번호와 함께 다시 보낸다.

4. **`etaxbr status <id>`의 nts 필드를 본다.** ISSUED만 보면 안 된다. 일 단위로 nts 미완료 건을 집계해 경보한다.

## 문제 구조 (추상화 코드)

① 문제 코드
```python
r = etax.issue(buyer_bizno=partner.bizno, items=items)
inv.set_done(r.id)                       # ISSUED 로 완료 처리
```
② 고친 코드
```python
r = etax.issue(buyer_bizno=re.sub(r'\D', '', partner.bizno), items=items)
inv.set_issued(r.id)
# 익일 배치: status(id).nts in {SENT} → done / REJECTED → 경보 + resend
```
무엇이 깨졌나: 2단계 처리(접수→국세청 전송)의 첫 단계 응답을 최종 완료로 썼고, 입력 정규화가 없었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 입력 정규화 + nts 상태 추적 | 작음 | 다른 형식 오류는 추적으로만 보임 | 기본 |
| 중계사에 접수 단계 검증 요청 | 협의 | 벤더 일정 | 보조 |

**결론**: 사업자번호는 숫자만 보내고, 완료는 국세청 전송 결과로 판정한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
