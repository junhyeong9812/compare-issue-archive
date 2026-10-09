# issue/flowctl/task-queue-name-env-prefix — 정리 (힌트)

태그: `deploy` · `config`

## 전체 흐름

```
API(시작): taskQueue = "${ENV}-merchant-review" → "prod2-merchant-review"
워커:      taskQueue = "prod-merchant-review" (설정 파일 하드코딩)
엔진: 큐에 태스크 쌓임, 폴러 0 → 시작은 성공, 진행 없음
```

## 핵심 문장

- 작업 큐는 **이름으로만 연결**된다 — 시작 측과 워커가 다른 이름을 쓰면 엔진은 오류 없이 태스크를 쌓기만 한다.
- 워커 설정이 환경 변수 대신 하드코딩돼 신규 리전에서 갈렸다.
- 대처: 큐 이름을 공용 상수 모듈에서만 생성, 큐별 폴러 수 0 경보.
