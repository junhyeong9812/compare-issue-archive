# probe — 바이너리 실행만으로 규칙을 알아내는 경로 (명령 8개)

케이스 폴더에서 실행. 출력은 실제 실행 원문(2026-10-09).

```
$ export MQPUB_SPOOL=$(mktemp -d)                                         # 1
$ vendor/bin/mqpub --help                                                 # 2
usage:
  mqpub publish -t <topic> [-f <file>]   read lines <key>TAB<payload>, publish as one batch
  mqpub consume -t <topic> [-o <offset>] print <offset>TAB<key>TAB<payload>
  mqpub stat -t <topic>
limits: key 1..64 bytes, payload 0..1024 bytes, 500 lines per batch
env: MQPUB_SPOOL (spool directory, required)

# 실제 앱이 만드는 키 두 개(같은 날 주문)를 한 배치로
$ printf 'ntf.ship.O261007-00001\ta\nntf.ship.O261007-00002\tb\n' | vendor/bin/mqpub publish -t probe   # 3
accepted 2 batch 1
$ vendor/bin/mqpub stat -t probe                                          # 4
topic probe batches 1 messages 1          ← accepted 2 인데 저장 1
$ vendor/bin/mqpub consume -t probe                                       # 5
0	ntf.ship.O261007-00001	a                ← 첫 메시지만 남음

# 경계 탐색: 앞 15바이트 같음 / 앞 16바이트 같음
$ printf 'aaaaaaaaaaaaaaaX\t1\naaaaaaaaaaaaaaaY\t2\n' | vendor/bin/mqpub publish -t probe     # 6
accepted 2 batch 2
$ printf 'aaaaaaaaaaaaaaaaX\t1\naaaaaaaaaaaaaaaaY\t2\n' | vendor/bin/mqpub publish -t probe   # 7
accepted 2 batch 3
$ vendor/bin/mqpub consume -t probe -o 1                                  # 8
1	aaaaaaaaaaaaaaaX	1
2	aaaaaaaaaaaaaaaY	2                       ← 15바이트 공통: 둘 다 저장
3	aaaaaaaaaaaaaaaaX	1                      ← 16바이트 공통: 첫 것만
```

(선택 확인) 같은 키를 다른 배치로 다시 보내면 저장된다 — `printf 'ntf.ship.O261007-00002\tb\n' | mqpub publish -t probe` → `accepted 1 batch 4`, stat `batches 4 messages 5`. 즉 범위는 배치 안.

결론: 한 publish(배치) 안에서 키의 앞 16바이트가 같으면 첫 메시지만 저장, 나머지는 exit 0·accepted=요청 건수로 조용히 버린다.
코드만 읽어서는 알 수 없다(래퍼는 accepted 를 그대로 믿고, 소비자는 읽은 것만 보낸다).
