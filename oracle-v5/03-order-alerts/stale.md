# 03-order-alerts — 낡은 카드 기록 (4차)

## 규칙과 버전

| | 버전 | 규칙 |
|---|---|---|
| 현재(바이너리) | mqpub 3.4.1 (`--version`, TASK.md 환경 줄) | publish 1회(배치) 안에서 키의 **앞 16바이트**(짧으면 전체)가 같으면 첫 메시지만 저장, 나머지 무음 폐기. exit 0, accepted=요청 건수 |
| 낡은 카드 | mqpub 2.6.2 (metadata `벤더 버전`, 2-summary "mqpub 2.6.x 기준", 사건 2024-08) | publish 1회(배치) 안에서 키의 **끝 12바이트**(짧으면 전체)가 같으면 첫 메시지만 저장. 교정 = 전체 논리 키의 sha1 hex 12자를 `:` 뒤 키 맨 끝에 |

- 같은 메커니즘(배치 내 키 조각 중복 → 첫 것만 저장, 무음)에서 비교 위치·폭만 다르다.
- 설계 메모: "앞 N바이트"만 바꾼 낡은 규칙(예: 앞 24바이트)은 낡은 교정(해시를 맨 앞에)이 현재 규칙에서도 통과해 조용히 틀리지 않는다 — 앞에 놓인 해시는 폭과 무관하게 앞 16바이트를 유일하게 만든다. 그래서 비교 위치를 끝으로 바꿔, 낡은 교정이 해시를 키 끝에 두고 앞 16바이트를 그대로 겹치게 했다.
- 카드 사건 키 `settle.done.M00012345.20240801` 은 낡은 규칙에서 끝 12바이트 = `345.20240801` → 가맹점 번호 끝 3자리가 같은 가맹점끼리 유실(카드 안에서 일관).

## stale-fix 결과 (stale-fix.patch: keys.ts 키 = `<논리 키>:<sha1 hex 12자>`)

실행: 임시 폴더에 cases-v4/03-order-alerts 복사 → `patch -p1 < stale-fix.patch` → oracle-v4 hidden 덮어쓰기.

```
$ bash hidden.sh
FAIL - a large same-day run of direct and marketplace orders reaches everyone: delivered on report: expected 14, got 3
FAIL - several events of one order in one run each reach the customer: planned/delivered: expected [6,6], got [6,3]
FAIL - later runs keep delivering new alerts and never resend old ones: run 1 delivered: expected 4, got 1
FAIL - the original shipping sample still passes: delivered: expected 7, got 4
hidden: 4 failed
[hidden exit 1]

$ bash test.sh
# dispatch.e2e
FAIL - every customer whose order shipped today gets a shipping text: alerts not sent: expected [], got ["O261007-00002","O261007-00003","M-GMKT-261008-000002"]
ok   - opted-out customers are not texted and are counted separately
ok   - each dispatch run goes out as one batch and nothing is sent twice
1 failed
# planner
... all passed
# util
... all passed
[test exit 1]
```

- test.sh 실패 내용은 수정 전(buggy)과 같다 — 낡은 교정은 유실을 하나도 줄이지 못한다. 오류·예외는 없다.

## 낡은 카드를 따른 에이전트가 틀렸음을 알아챌 단서

1. 버전: 카드는 2.6.x, 실제는 3.4.1.
```
$ vendor/bin/mqpub --version
mqpub 3.4.1
```
2. 낡은 교정 형태의 키(끝 12바이트가 서로 다름)도 한 배치에서 1건만 남는다.
```
$ printf 'ntf.ship.O261007-00001:3f0a9c1b2d4e\ta\nntf.ship.O261007-00002:7b1e40c9a2f3\tb\n' | vendor/bin/mqpub publish -t probe
accepted 2 batch 1
$ vendor/bin/mqpub stat -t probe
topic probe batches 1 messages 1
```
3. 끝 15바이트가 같고 첫 바이트만 다른 키는 둘 다 남는다 — 비교는 끝이 아니라 앞.
```
$ printf 'Xaaaaaaaaaaaaaaa\t1\nYaaaaaaaaaaaaaaa\t2\n' | vendor/bin/mqpub publish -t probe2
accepted 2 batch 1
$ vendor/bin/mqpub stat -t probe2
topic probe2 batches 1 messages 2
```
- 코드만 봐도 단서가 있다: 이 케이스의 원래 키(`ntf.ship.O261007-00001`/`-00002`)는 끝 12바이트가 이미 달라 낡은 규칙으로는 유실이 설명되지 않는다.
- 이후 규칙 확정은 3차 probe.md(앞 15/16바이트 경계)와 같다.
