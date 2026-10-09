# issue/pgroute/circuit-breaker-config-key-rename — 정리 (힌트)

태그: `config-drift` · `deploy`

## 전체 흐름

```
2025-07-01 배포: 회복성 라이브러리 1.x → 2.x
  옛 키  failureRateThreshold: 30, ringBufferSizeInClosedState: 50
  새 키  slidingWindowSize   ← 옛 키는 조용히 무시, 기본값 100 적용
      │
07-15 PG-C 장애: 실패율 100% 지만 최소 호출 수(기본) 조건·창 크기 차이로 개방 지연
      ▼
라우터가 PG-C 를 계속 후보로 유지
```

## 핵심 문장

- 설정 바인딩은 **모르는 키를 무시**한다 — 오타·이름 변경이 오류가 아니다.
- 배포 후 실제 적용 값을 확인하는 단계가 없었다.
- 교정: 서킷 상태·적용 설정값을 운영 엔드포인트로 노출하고 배포 체크에 넣는다. 모르는 키는 기동 실패로 처리한다.
