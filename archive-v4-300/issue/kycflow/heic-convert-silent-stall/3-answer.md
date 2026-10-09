# issue/kycflow/heic-convert-silent-stall — 정답

태그: `silent-failure` · `environment-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **들어가지 않을 수 있다.** 신 이미지 노드에 배정되면 변환 예외가 삼켜져 `UPLOADED`에 머문다.

2. **워커 6대 중 2대만 새 이미지였기 때문이다.** 구 이미지 노드에 배정된 건은 정상 변환됐다. 11월 3일~10일 HEIC 업로드 541건 중 177건이 멈췄다.

3. **없었다.** JPG·PDF는 변환 단계를 건너뛴다. 그래서 전체 심사 지연 지표에는 작게만 보였다.

4. **예외 처리가 `catch (Exception e) { log.debug(...) }`였기 때문이다.** 운영 로그 수준이 info라 기록조차 남지 않았다.

5. **코드**: 변환 실패 시 `CONVERT_FAILED`로 바꾸고 다른 노드에서 1회 재시도, 그래도 실패면 고객에게 재업로드 안내.\
**배포 검증**: HEIC·PNG·JPG·PDF 샘플 변환 스모크를 이미지 빌드 파이프라인에 넣는다.

## 문제 구조 (추상화 코드)

### 변형 A — 변환 예외 삼킴
① 문제 코드
```java
try {
    doc = converter.toJpeg(doc);
    doc.setStatus(QUEUED);
} catch (Exception e) {
    log.debug("convert skipped", e);     // 상태 그대로
}
```
② 고친 코드
```java
try {
    doc = converter.toJpeg(doc);
    doc.setStatus(QUEUED);
} catch (ConvertException e) {
    doc.setStatus(CONVERT_FAILED);
    metrics.increment("kyc.convert.fail", "fmt", doc.format());
    retryQueue.submit(doc, otherNode());  // 1회 재시도
}
```
무엇이 깨졌나: 실행 환경 차이로 생긴 예외를 삼켜 상태 전이가 멈췄고, 실패가 어디에도 남지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 실패 상태 + 재시도 | 코드 수정 | 작음 | 없음 | 기본 |
| 이미지 빌드 포맷 스모크 | 샘플 파일 | 작음 | 새 포맷 누락 | 함께 |
| UPLOADED 체류 30분 경보 | 지표 | 작음 | 피크 지연 오탐 | 안전망 |

**결론**: 상태 전이가 일어나지 않는 실패를 없앤다. 예외는 상태로 남긴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
