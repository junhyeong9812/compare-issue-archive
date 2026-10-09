# issue/nodepool/scale-down-evicts-settlement-batch — 정리 (힌트)

태그: `duplicate` · `batch`

## 전체 흐름

```
노드 사용률 40% → nodepool 축소 후보 → 파드 축출
Job 재시작 → 처음부터 → 같은 파일에 append
파일: 앞 50분치 줄 2번 → 은행 지급 → 중복
```

## 핵심 문장

- 오토스케일러는 **PDB·축출 금지 주석이 없는 파드**를 옮길 수 있다고 본다.
- 배치가 **임시 파일에 쓰고 마지막에 교체**하지 않고 최종 파일에 append 했다.
- 은행은 줄 단위 중복을 걸러 주지 않는다.
- 배치 파드에 `safe-to-evict: false`, 출력은 임시 파일→원자 교체, 지급 키로 중복 차단.
