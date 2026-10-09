# issue/batchd/restart-checkpoint-read-offset-skip — 정답

태그: `silent-failure` · `restartability`

## 정답
<!-- 질문 1:1 대응 -->

1. **33,001부터다.** 30,001~33,000은 처리되지 않고 상태는 COMPLETED로 끝난다.

2. **3.5는 선읽기가 없어 읽은 위치 = 커밋 위치였다.** 3.6 릴리스 노트의 "prefetch 기본 3"이 원인이었다.

3. **없다.** 중단·재시작이 있었던 회차만 선읽기분이 빠진다 — 그래서 "가끔" 일어났다.

4. **실행 이력에서 재시작 여부를 보고, 누락 키가 연속 구간인지 본다.** 재시작 지점 직전의 연속 구간이면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — 리더 위치를 체크포인트로 사용
① 문제 코드
```yaml
step: expire-points
chunk: 1000
reader: { prefetch: 3 }
checkpoint: reader          # 3.6 기본값
```
② 고친 코드
```yaml
step: expire-points
chunk: 1000
reader: { prefetch: 3 }
checkpoint: commit          # 마지막 커밋 키
verify: "SELECT count(*) FROM expire_target WHERE run_id=:run AND done=false" == 0
```
무엇이 깨졌나: "읽었다"와 "처리했다"를 같은 위치로 기록했다 — 선읽기가 둘을 갈랐다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 커밋 기준 체크포인트 | 정렬 키 존재 | 작음 | 없음 | 기본 |
| 완료 후 대상=처리 건수 검증 | 대상 테이블 | 작음 | 없음 | 함께 적용 |
| 선읽기 끄기 | 없음 | 성능 저하 | 없음 | 임시 조치 |

**결론**: 재시작 지점은 커밋된 위치로만 잡고, 완료 판정은 대상 건수로 검증한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
