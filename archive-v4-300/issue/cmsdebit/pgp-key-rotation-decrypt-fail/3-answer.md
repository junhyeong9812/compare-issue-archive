# issue/cmsdebit/pgp-key-rotation-decrypt-fail — 정답

태그: `credential-rotation` · `fail-closed`

## 정답
<!-- 질문 1:1 대응 -->

1. **만료일이 키 파일 안에만 있고 관리 대장이 없었다.**

2. **된다.** 대행사가 재암호화해 재전송했고 `cmsdebit result fetch --date`로 다시 받았다.

3. **펌뱅킹·매입 수집의 키도 대장에 올려 만료 30일 전 경보를 걸었다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```sh
gpg --decrypt result.pgp > result.txt
```
② 고친 코드
```sh
./ops/key-ledger-check --warn-days 30   # 키 대장 점검 (일 배치)
gpg --decrypt result.pgp > result.txt || alert cmsdebit-decrypt
```
무엇이 깨졌나: 암호 키의 수명 관리가 없었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 키 대장 + 만료 경보 | 작음 | 대장 누락 | 기본 |
| 만료 없는 키 | 없음 | 보안 정책 위반 | 선택하지 않음 |

**결론**: 키는 만료일을 대장으로 관리한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
