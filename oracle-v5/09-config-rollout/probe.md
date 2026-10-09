# probe — cfgsvc 반영 규칙을 실행만으로 알아내는 경로

전제: 케이스 폴더 cwd, `export CFGSVC_HOME=$(mktemp -d)`. 명령 10개(`--help` 포함).
B가 실제로 밟을 법한 앞 단계(선택): 테스트 실패에서 api 풀만 옛 값 → 배포가 끝난 뒤 `cfgsvc get pool.api.db_pool_size`는 새 값(40)인데
렌더 파일은 옛 값(20) → 렌더 시점에는 옛 값이 나왔다는 뜻 → 캐시·범위 해석을 의심해 보다가, 배포 단계 중 서비스에 손대는 것은
apply(set)·record(mark)뿐임을 확인 → 아래처럼 set 직후 get을 직접 확인.

판단 포인트:
- 2·3: 처음 만드는 키는 set 직후 get이 새 값을 준다 — 그래서 기준 배포(전부 새 키)와 새 키만 바꾼 batch 변경은 정상이었다.
- 5·6: 이미 값이 있는 키는 set이 exit 0·변경 번호를 줘도 get은 이전 값을 준다.
- 7: `keys`의 마지막 칸이 `3` — 아직 get에 보이지 않는 변경. (반영된 키는 `0`)
- 8·9·10: `mark`(도움말에는 "현재 마크 출력"이라고만 적힘)를 한 번 부르면 마크 번호가 오르고, 그 뒤 get이 새 값을 주며 st가 `0`이 된다.
  (변경이 없을 때 `mark`를 다시 부르면 같은 번호가 나온다.)
- 결론: 기존 키의 변경은 다음 `mark` 전까지 get에 보이지 않는다(오류 없음). 배포의 record 단계가 마지막에 mark를 부르므로,
  배포가 끝난 뒤 조회하면 새 값이 보이지만 렌더 시점에는 옛 값이었다.
  대처: set을 다 한 뒤, get으로 읽기(렌더) 전에 `cfgsvc mark`를 부른다.

## 실행 기록 (실제 출력)

```
$ vendor/bin/cfgsvc --help
usage: cfgsvc <command> [args]

commands:
  set   KEY VALUE    prints the change number
  get   KEY          prints the value
  keys  [PREFIX]     key, change, st (tab separated)
  mark               prints the current mark

environment:
  CFGSVC_HOME   state directory (required)
[exit 0]
$ vendor/bin/cfgsvc set probe.x 1
C0000001
[exit 0]
$ vendor/bin/cfgsvc get probe.x
1
[exit 0]
$ vendor/bin/cfgsvc mark
M0000001
[exit 0]
$ vendor/bin/cfgsvc set probe.x 2
C0000002
[exit 0]
$ vendor/bin/cfgsvc get probe.x
1
[exit 0]
$ vendor/bin/cfgsvc keys probe.
probe.x	C0000002	3
[exit 0]
$ vendor/bin/cfgsvc mark
M0000002
[exit 0]
$ vendor/bin/cfgsvc get probe.x
2
[exit 0]
$ vendor/bin/cfgsvc keys probe.
probe.x	C0000002	0
[exit 0]
```
