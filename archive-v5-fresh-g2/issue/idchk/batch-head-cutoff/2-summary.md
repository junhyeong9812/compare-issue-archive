# issue/idchk/batch-head-cutoff — 정리 (힌트)

> 유효: idchk 3.2.x — 확인: `idchk --version`

태그: `silent-failure` · `resource-bounding` · `contract-drift`

## 전체 흐름 (idchk 3.2.x 기준)

```
배치 ──batch --in FILE (N줄)──▶ idchk
                                 │ 줄 전체 형식 검사 (하나라도 틀리면 E13~E17, 파일 전체 거부)
                                 │
                 1 ~ 12번째 줄    │ 판정 저장 ──▶ result <ref> : "v 1" / "v 2"
                 13번째 줄 ~      │ 버림      ──▶ result <ref> : E31 (영구)
                                 │
                                 └─ 항상: stdout "J0000xxx<TAB>N" (N = 읽은 전체 줄 수), exit 0, stderr 없음

job J0000xxx  →  in N / v1 a / v2 b      a + b = min(N, 12)   ← 버림이 보이는 유일한 곳

한 번에 보내는 호출부
  20줄 → 12 판정 + 8 버림 → 8건은 result E31 → "처리 중"으로 오인 → 재전송 안 함 → 영구 대기
올바른 호출부
  12줄 이하로 나눠 여러 번 batch → 전 건 판정
  확인: 요청마다 result 가 나오는지 (또는 job 의 v1 + v2 == in)
```

## 핵심 문장

- `idchk batch`는 한 번 호출에 **앞 12줄만 판정**하고 13번째 줄부터는 **흔적 없이 버린다** — exit 0, 출력의 줄 수는 넣은 전체 건수.
- 버려진 요청은 `result`가 영영 **E31**이다 — "처리 중"이 아니라 "받은 적 없음"이다.
- 버림은 `job`의 **v1 + v2 < in**으로만 보인다 — 접수 응답(job id·줄 수)으로는 알 수 없다.
- 대처는 **호출당 12줄 이하로 나눠 보내기** — 버려진 줄을 다시 보내면 정상 판정된다.
- 완료 판정은 접수가 아니라 **요청별 result 존재**로 한다.
