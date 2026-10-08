# probe — 바이너리 실행만으로 규칙 알아내기 (명령 10개)

케이스 폴더에서 실행. 정산이 틀린 날(10/03~10/05)과 맞는 날(10/02)을 비교하는 것이 출발점이다.
(재설계 v2: `--fix` → 의미 없는 `--src N`. 소스 번호의 뜻은 바이너리·help 어디에도 없다.)

```
$ vendor/bin/fxq --help
usage: fxq rate <PAIR> <YYYY-MM-DD> [--src N]
       fxq pairs
       fxq --version
options:
  --src N   quote source 0..3 (default 0)
$ vendor/bin/fxq rate USDKRW 2026-10-02
USDKRW 2026-10-02 1386.02
$ vendor/bin/fxq rate USDKRW 2026-10-03
USDKRW 2026-10-03 1386.02 ~1
$ vendor/bin/fxq rate USDKRW 2026-10-05
USDKRW 2026-10-05 1386.02 ~3
$ vendor/bin/fxq rate USDKRW 2026-10-03 --src 1
USDKRW 2026-10-03 1374.05
$ vendor/bin/fxq rate USDKRW 2026-10-03 --src 2
USDKRW 2026-10-03 1383.61
$ vendor/bin/fxq rate USDKRW 2026-10-03 --src 3
USDKRW 2026-10-03 1379.33
$ vendor/bin/fxq rate USDKRW 2026-10-02 --src 1
USDKRW 2026-10-02 1381.12
$ vendor/bin/fxq rate USDKRW 2026-10-02 --src 2
USDKRW 2026-10-02 1390.11
$ vendor/bin/fxq rate USDKRW 2026-10-02 --src 3
USDKRW 2026-10-02 1386.02
```

알아낼 수 있는 것:
- 10/03~10/05 기본 조회는 10/02 값이 그대로 나오고 `~1`~`~3`이 붙는다(10/05는 월요일인데도 붙는다 → 요일 규칙이 아님).
- 소스 1·2·3은 꼬리표 없이 서로 다른 값을 낸다. 어느 것이 정산값인지는 help에 없다.
- 판별: 10/02(이미 파트너 명세와 맞는 날)에 소스 1·2는 기본값과 다르고 소스 3만 같다 → 1·2로 정산하면 맞던 평일이 깨진다. 소스 3이 유일한 후보.
- 10/03 소스 3 값(1379.33)은 직전값(1386.02)·앞뒤 영업일(1386.02·1373.04) 평균 1379.53·소스 1·2 평균 1378.83 어느 것도 아니다 → 직접 계산 불가.
- 사용자 코드(FxqClient.parse)가 4번째 칸을 버리는 것과 결합해 원인 확정. 수정 후 `api.Main reconcile 2026-10-01 2026-10-05` → `OK - matches partner statements`로 확인.
- hidden 대비: 10/09(금, 한글날)도 꼬리표가 붙는다 — 요일·날짜 하드코딩이 아니라 꼬리표로 판정해야 한다.
