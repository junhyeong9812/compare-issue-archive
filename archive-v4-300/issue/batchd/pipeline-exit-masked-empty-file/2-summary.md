# issue/batchd/pipeline-exit-masked-empty-file — 정리 (힌트)

태그: `silent-failure` · `exit-code`

## 전체 흐름

```
step settle-export (sh):
  extract --date $D | transform --merchant $M | gzip > out/$M.gz
       │ extract 가 DB 읽기 타임아웃 → exit 3, 출력 0줄
       ▼
  transform: 빈 입력 → 정상 종료 / gzip: 빈 입력 압축 → exit 0
batchd: 스텝 결과 = 파이프라인 종료 코드 = gzip 의 0 → SUCCESS
전송 스텝: 20바이트짜리 빈 gz 를 가맹점에 전송
```

## 핵심 문장

- 셸 파이프라인의 종료 코드는 **마지막 명령의 것**이다 — `pipefail` 없이는 앞 단계 실패가 사라진다.
- `batchd`의 셸 스텝은 `/bin/sh -c`로 실행되고 `set -o pipefail`을 넣지 않는다.
- DB가 붐빈 월초에만 extract가 타임아웃 나서 "가끔, 일부 가맹점만" 빈 파일이 됐다.
- 대처: 스텝 앞에 `set -euo pipefail`, 산출물 검증 스텝(줄 수 > 0, 건수 = 원장 건수) 추가.
