# issue/tracepipe/exporter-queue-full-silent-drop — 정답

태그: `silent-failure` · `observability`

## 정답
<!-- 질문 1:1 대응 -->

1. **버려진다.**

2. **수신부는 받은 즉시 성공을 돌려준다.** 드롭은 내보내기 단계의 메트릭에만 보였다.

3. **오지 않는다.** 메모리 큐였다.

4. **큐 사용률 80%·enqueue 실패 0 초과 경보.** 영속 큐(디스크 20GB)로 바꿨다.

## 문제 구조 (추상화 코드)

### 변형 A — 메모리 큐 기본값
① 문제 코드
```yaml
exporters:
  otlp:
    sending_queue: { enabled: true, queue_size: 5000 }
```
② 고친 코드
```yaml
extensions: { file_storage: { directory: /var/lib/tracepipe/queue } }
exporters:
  otlp:
    sending_queue: { enabled: true, queue_size: 50000, storage: file_storage }
```
무엇이 깨졌나: 수집기 수신 성공을 저장 성공으로 봤다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 영속 큐 | 디스크 | 작음 | 디스크 가득 | 기본 |
| 드롭 경보 | 메트릭 | 작음 | 없음 | 함께 |
| 큐 크기만 증대 | 메모리 | 작음 | 재기동 시 손실 | 보조 |

**결론**: 영속 큐와 드롭 경보를 함께 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
