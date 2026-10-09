# issue/cdcpipe/slot-recreated-snapshot-never-gap — 정리 (힌트)

태그: `silent-failure` · `data-loss`

## 전체 흐름

```
07-20 23:10 DBA: 디스크 경보로 비활성 슬롯 cdc_stats 삭제 (커넥터는 배포 중 정지 상태)
07-21 00:40 커넥터 기동: 슬롯 없음 → 새로 생성 (현재 LSN)
              snapshot.mode=never → 초기 스냅샷 생략
23:10 ~ 00:40 사이 커밋된 변경 6,412건 → 어디에도 전달 안 됨
커넥터 상태: RUNNING, 오류 없음
```

## 핵심 문장

- 논리 복제 슬롯은 **커넥터의 읽기 위치를 보존**하는 장치다 — 삭제되면 그 사이 변경을 다시 받을 방법이 없다.
- `snapshot.mode=never`라 슬롯을 새로 만들면서 빈 구간을 메우지 않았다.
- 커넥터는 저장된 오프셋(LSN)이 새 슬롯보다 앞이라는 점을 경고 없이 넘겼다.
- 대처: `snapshot.mode=when_needed`, 슬롯 삭제는 커넥터 담당자 승인 절차로, 일 건수 대사 자동화.
