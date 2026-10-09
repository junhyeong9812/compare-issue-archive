# stale — 09-config-rollout (cfgsvc)

## 규칙

| | 버전 | 보류 대상 | 대처 |
|---|---|---|---|
| 현재(바이너리) | cfgsvc 2.3.0 (`--version`) | 마지막 mark 때 **이미 값이 있던 키**의 변경 — 다음 mark 전까지 get이 이전 값(새 키는 바로 보임, keys st 3) | set 묶음 **뒤**, 읽기(렌더) 전에 mark |
| 낡은 카드 | cfgsvc 1.8.x (사건 2024-06) | 마지막 mark 뒤 **이미 변경이 들어간 키**의 재변경만 — mark 뒤 첫 set은 기존 키도 바로 보임(st 0) | set 묶음 **앞**에서 mark로 보류를 비움 + 같은 키는 한 번만 set |

같은 메커니즘(mark = get이 내주는 값의 기준점, `keys` st 3 = 보류)에서 조건(보류 대상)만 다르고, 그 결과 mark 위치가 set 뒤 ↔ set 앞으로 갈린다.
낡은 카드대로 하면 존재하는 명령(`mark`)만 쓰고 오류도 없지만, mark 뒤 기존 키의 첫 변경이 그대로 보류되어 렌더가 옛 값이다.

## stale-fix.patch 결과

`steps/20-apply.sh` 루프 **앞**에 `cfg_mark` 한 번(3차 fix.patch와 같은 파일, 위치만 낡은 규칙대로 set 앞).
- `bash test.sh` → **실패** (exit 1, `# test_rollout.sh: 17 passed, 6 failed` — api 풀 기존 키가 여전히 옛 값으로 렌더. 원래 버그와 같은 증상).
- `bash hidden.sh` → **실패** (exit 1, `# hidden: 27 failure(s)` — 예: `h3 pay-02 db_pool_size rendered [12], service [14], expected [14]`).
- 즉 낡은 카드를 따른 수정은 "아무것도 안 고친 것"과 같다. 보이는 테스트가 바로 잡아 주므로 에이전트는 카드와 현실의 불일치를 탐침으로 확인해야 한다.

## 틀렸음을 알아챌 단서 (탐침, cwd = 케이스, `export CFGSVC_HOME=$(mktemp -d)`)

```
$ vendor/bin/cfgsvc --version            # 1 — 카드는 1.8.x, 지금은 2.3.0
cfgsvc 2.3.0
[exit 0]
$ vendor/bin/cfgsvc set probe.x 1
C0000001
[exit 0]
$ vendor/bin/cfgsvc mark
M0000001
[exit 0]
$ vendor/bin/cfgsvc set probe.x 2        # 2 — mark 뒤 첫 변경(카드라면 바로 보여야 함)
C0000002
[exit 0]
$ vendor/bin/cfgsvc get probe.x          #   → 이전 값 1 (카드와 모순)
1
[exit 0]
$ vendor/bin/cfgsvc keys probe.          # 3 — mark 뒤 첫 변경인데 st 3 (카드는 st 0)
probe.x	C0000002	3
[exit 0]
```

그 뒤 `mark` → `get probe.x` = 2 로 현재 규칙(set 뒤 mark)이 확인된다(oracle-v3 probe.md 8~10과 같다).
