# issue/iamctl/group-deny-precedence — 정답

태그: `security` · `policy`

## 정답
<!-- 질문 1:1 대응 -->

1. **거부된다.** 명시 deny가 allow보다 우선한다.

2. **정상인 상담원은 이전 전환 때 legacy 그룹에서 이미 빠져 있었다.** 2024년 이후 전환된 14명만 남아 있었다.

3. **`deny by outsourced-legacy:stmt-2`처럼 결정한 그룹과 문장을 보여 준다.** 처음 문의 때 이것부터 봤으면 10분이면 됐다(실제 반나절).

4. **2023년 외주 계약 종료 때 기존 계정의 고객 조회를 막으려고 넣었다.** 그룹을 지우는 대신 deny를 붙였다.

5. **개편 변경에 '이전 그룹 제거'를 같이 넣고, 적용 전 `explain`으로 대표 사용자를 시험한다.** 계약 종료 그룹은 deny 대신 그룹 삭제로 처리한다.

## 문제 구조 (추상화 코드)

### 변형 A — 그룹 추가만
① 문제 코드
```sh
iamctl group add-member cs-team2 cs0733
```
② 고친 코드
```sh
iamctl group add-member cs-team2 cs0733
iamctl group remove-member outsourced-legacy cs0733
iamctl explain --user cs0733 --action customer:read | grep -q '^allow' || exit 1
```
무엇이 깨졌나: 구 정책을 그룹 삭제가 아니라 deny로 막아 두어, 그 그룹에 남은 사람이 새 권한을 받아도 계속 막혔다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 개편 시 이전 그룹 제거 | 소속 목록 | 작음 | 누락 | 기본 |
| deny 그룹 폐기·삭제 | 영향 조사 | 중간 | 다른 사용 경로 | 함께 |
| explain 시험 단계 | CLI | 작음 | 대표 표본만 | 보조 |

**결론**: 거부는 그룹을 남겨 두는 방식으로 쓰지 않는다. 개편은 추가와 제거를 함께 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
