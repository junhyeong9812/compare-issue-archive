# issue/metricdb/out-of-order-after-vm-restore — 정답

태그: `silent-failure` · `time`

## 정답
<!-- 질문 1:1 대응 -->

1. **거부된다.** `out_of_order_samples_total`만 오른다.

2. **HTTP 응답은 성공이고 개별 샘플 거부는 본문에만 있었다.**

3. **정상화되지만 3시간 공백이 남는다.**

4. **부팅 후 `chronyc waitsync` 통과 뒤 에이전트 시작.**

## 문제 구조 (추상화 코드)

### 변형 A — 시간 동기화 전 에이전트 기동
① 문제 코드
```ini
[Unit]
After=network.target
```
② 고친 코드
```ini
[Unit]
After=network.target chrony-wait.service
Requires=chrony-wait.service
```
무엇이 깨졌나: 시계가 늘 맞다고 가정했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 동기화 후 기동 | systemd 의존 | 작음 | 동기화 실패 시 미기동 | 기본 |
| 거부 카운터 경보 | 메트릭 | 작음 | 없음 | 함께 |
| out-of-order 허용 창 | DB 설정 | 작음 | 메모리 증가 | 보조 |

**결론**: 복구 런북에 시간 동기화 확인을 넣고 거부 카운터를 감시한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
