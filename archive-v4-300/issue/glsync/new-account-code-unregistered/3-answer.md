# issue/glsync/new-account-code-unregistered — 정답

태그: `config-drift` · `release-process`

## 정답
<!-- 질문 1:1 대응 -->

1. **스테이징 ERP는 계정 마스터 검증을 끈 상태였다.** 환경 차이가 숨겼다.

2. **다음 재시도 주기(10분)에 전부 반영됐다.** 전표 번호 멱등이라 중복은 없었다.

3. **새 계정과목 목록과 ERP 조회 결과 대조.** 스테이징 ERP 검증도 켰다.

## 문제 구조 (추상화 코드)

① 문제 코드
```python
deploy(rules)                          # 분개 규칙만 배포
```
② 고친 코드
```python
missing = [a for a in rules.accounts() if not erp.account_exists(a)]
if missing: raise PreflightError(missing)
deploy(rules)
```
무엇이 깨졌나: 외부 마스터 데이터 의존성이 출시 절차에 없었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 배포 전 계정 존재 확인 | 작음 | 없음 | 기본 |
| 미등록 계정 임시 계정으로 보내기 | 작음 | 임시 계정 정리 부담 | 선택하지 않음 |

**결론**: 외부 마스터 의존은 배포 전 확인으로 막는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
