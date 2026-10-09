# issue/tracepipe/exporter-queue-full-silent-drop — 정리 (힌트)

태그: `silent-failure` · `observability`

## 전체 흐름

```
앱 → 수집기 수신(OK) → 내보내기 큐 [5000/5000] → 백엔드(느림)
큐 가득 → enqueue 실패 → 드롭 (otelcol_exporter_enqueue_failed_spans 증가)
```

## 핵심 문장

- 수신 성공은 **큐 적재 시도 전** 응답이다 — 내보내기 실패는 앱에 전달되지 않는다.
- 큐가 차면 새 스팬은 버려지고 **메트릭 카운터만** 오른다.
- 기본 큐는 메모리라 버린 스팬은 복구되지 않는다 — 영속 큐(파일 저장)를 쓰면 재시도할 수 있다.
- `enqueue_failed_spans`와 큐 사용률에 경보를 건다.
