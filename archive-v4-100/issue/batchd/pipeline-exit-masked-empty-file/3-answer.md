# issue/batchd/pipeline-exit-masked-empty-file — 정답

태그: `silent-failure` · `exit-code`

## 정답
<!-- 질문 1:1 대응 -->

1. **SUCCESS다.** 파이프라인 마지막 명령인 gzip이 0으로 끝났기 때문이다. 산출 파일은 헤더만 있는 약 20바이트 gz다.

2. **월초 집계 쿼리와 겹쳐 DB 읽기가 느려져 extract 타임아웃(120초)을 넘었다.** 평소에는 40초 안에 끝났다.

3. **아니다.** `set -e`는 파이프라인 중간 실패를 보지 않는다 — `pipefail`이 있어야 extract의 3이 스텝 결과가 된다.

4. **차액이 난 가맹점의 전송 파일 크기와 줄 수를 본다.** 0줄 파일이면 이 사건, 줄은 있는데 금액이 다르면 집계 로직 쪽이다.

## 문제 구조 (추상화 코드)

### 변형 A — pipefail 없는 셸 스텝
① 문제 코드
```sh
extract --date "$D" | transform --merchant "$M" | gzip > "out/$M.gz"
```
② 고친 코드
```sh
set -euo pipefail
extract --date "$D" | transform --merchant "$M" | gzip > "out/$M.gz"
n=$(zcat "out/$M.gz" | wc -l)
expected=$(ledger count --date "$D" --merchant "$M")
[ "$n" -eq "$expected" ] || { echo "rows $n != $expected"; exit 4; }
```
무엇이 깨졌나: 스텝 성공을 마지막 명령의 종료 코드로 판정했고, 산출물 자체는 검증하지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| pipefail + 산출물 건수 검증 | 원장 건수 조회 | 작음 | 없음 | 기본 |
| batchd 셸 스텝 기본 옵션 변경 | 실행기 설정 | 중간 | 기존 스텝 동작 변화 | 전사 적용 시 |

**결론**: 파이프라인은 pipefail로 실행하고, 성공 판정은 산출물 건수로 한 번 더 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
