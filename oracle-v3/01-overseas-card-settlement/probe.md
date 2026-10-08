# probe — 바이너리 실행만으로 규칙 알아내기 (명령 10개)

케이스 폴더에서 실행. 정산이 틀린 날(10/03~10/05)과 맞는 날(10/02)을 비교하는 것이 출발점이다.

```
$ vendor/bin/fxq --help
usage: fxq rate <PAIR> <YYYY-MM-DD> [--fix]
       fxq pairs
       fxq --version
$ vendor/bin/fxq rate USDKRW 2026-10-02
USDKRW 2026-10-02 1386.02
$ vendor/bin/fxq rate USDKRW 2026-10-03
USDKRW 2026-10-03 1386.02 ~1
$ vendor/bin/fxq rate USDKRW 2026-10-05
USDKRW 2026-10-05 1386.02 ~3
$ vendor/bin/fxq rate USDKRW 2026-10-06
USDKRW 2026-10-06 1373.04
$ vendor/bin/fxq rate USDKRW 2026-10-03 --fix
USDKRW 2026-10-03 1379.33
$ vendor/bin/fxq rate USDKRW 2026-10-02 --fix
USDKRW 2026-10-02 1386.02
$ vendor/bin/fxq rate USDKRW 2026-10-09
USDKRW 2026-10-09 1377.48 ~1
$ vendor/bin/fxq rate USDKRW 2026-10-09 --fix
USDKRW 2026-10-09 1377.55
$ vendor/bin/fxq rate EURKRW 2026-10-04 --fix
EURKRW 2026-10-04 1612.02
```

알아낼 수 있는 것:
- 10/03~10/05는 10/02 값이 그대로 나오고 `~1`~`~3`이 붙는다(10/05는 월요일인데도 붙는다 → 요일 규칙이 아님).
- `--fix`를 붙이면 꼬리표 없이 다른 값이 나오고, 꼬리표 없는 날(10/02)은 `--fix`여도 값이 같다.
- 10/03 `--fix` 값(1379.33)은 10/02(1386.02)·10/06(1373.04) 어느 쪽도, 평균(1379.53)도 아니다 → 직접 계산 불가.
- 사용자 코드(FxqClient.parse)가 4번째 칸을 버리는 것과 결합하면 원인이 확정된다.
- 검증: 수정 후 `api.Main reconcile 2026-10-01 2026-10-05` → `OK - matches partner statements`. (10/09 금요일도 휴장 — hidden 대비)
