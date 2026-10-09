# issue/kycflow/heic-convert-silent-stall — 정리 (힌트)

태그: `silent-failure` · `environment-drift`

## 전체 흐름

```
업로드 ──▶ 상태 UPLOADED ──▶ 전처리 워커 (6대, 라운드로빈)
                               │ HEIC → JPG 변환 (libheif)
           구 이미지 4대        │ 성공 → 상태 QUEUED → 심사 큐
           신 이미지 2대        │ libheif 없음 → 예외
                               │   catch(Exception) { log.debug } ← 삼킴
                               └   상태 UPLOADED 그대로, 재시도 없음

영향 = HEIC 업로드 × 신 이미지 노드 배정 (약 1/3)
JPG·PDF = 변환 불필요 → 영향 없음
```

## 핵심 문장

- 새 베이스 이미지에 **libheif가 빠져** HEIC 변환이 실패했다.
- 워커가 변환 예외를 **debug 로그로 삼키고** 상태를 바꾸지 않았다 — 실패도 재시도도 없다.
- 라운드로빈 배정이라 **같은 고객도 어떤 건은 정상**이었다 — '일부'는 노드 비율의 흔적이다.
- 실패는 상태(`CONVERT_FAILED`)와 경보로 드러내고, `UPLOADED` 체류 시간을 감시한다.
- 베이스 이미지 교체 검증에 지원 포맷별 변환 스모크를 넣는다.
