# issue/flowctl/heartbeat-timeout-restart-loop — 정리 (힌트)

태그: `performance` · `timeout`

## 전체 흐름

```
activity renderStatement: heartbeatTimeout=30s, startToClose=2h
코드: 하트비트 없음 → 30초 후 엔진이 실패 처리 → 재시도(처음부터)
워커는 실제로 계속 렌더링 중 → 이전 시도와 새 시도가 동시에 돌기도 함
```

## 핵심 문장

- 하트비트 시간 초과를 설정한 단계는 **그 간격 안에 하트비트를 보내야** 살아 있는 것으로 본다.
- 소형 내역서는 30초 안에 끝나 문제가 없었고, 법인 연간 내역서(수만 쪽)만 걸렸다.
- 대처: 페이지 1,000장마다 하트비트 + 진행 위치를 하트비트 세부값에 담아 재시도 시 이어서 처리.
