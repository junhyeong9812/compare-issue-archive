# issue/batchd/restart-checkpoint-read-offset-skip — 정리 (힌트)

태그: `silent-failure` · `restartability`

## 전체 흐름

```
chunk=1000, reader 가 미리 3청크(3000건)를 읽어 버퍼링
  read offset 저장 → 33,000
  커밋 완료 → 30,000 까지
노드 재배치로 중단
재시작: offset 33,000 부터 읽음 → 30,001~33,000 건 영영 처리 안 됨
batchd: 최종 상태 COMPLETED
```

## 핵심 문장

- `batchd` 3.6은 성능 개선으로 **선읽기(prefetch)** 를 넣으면서 체크포인트를 리더의 위치로 저장했다.
- 커밋되지 않은 선읽기분이 재시작 때 "이미 처리됨"으로 취급돼 건너뛰어졌다.
- 소멸 대상 3,000명이 빠졌고, 회계 쪽 소멸 충당금과 대사 차액으로 드러났다.
- 대처: 체크포인트를 **커밋된 마지막 키**로 저장하도록 스텝 설정(`checkpoint=commit`)으로 변경, 완료 후 대상 건수 = 처리 건수 검증.
