# issue/bidash/exec-dashboard-cache-ttl-correction — 정답

태그: `cache` · `data-freshness` · `communication`

## 정답
<!-- 질문 1:1 대응 -->

1. **질의 결과 캐시가 07:00에 만들어져 24시간 유지됐다.**

2. **경영 대시보드 질의가 무거워 웨어하우스 부하를 줄이려 했다.**

3. **분석 리포트는 캐시 1시간이었다.**

4. **적재 완료 이벤트로 캐시 무효화, 화면에 "데이터 기준 시각" 표시, 정정 시 공지 채널 알림.**

## 문제 구조 (추상화 코드)

### 변형 A — 데이터 변경과 무관한 고정 캐시
① 문제 코드
```yaml
dashboard: exec_daily
cache_ttl: 24h
```
② 고친 코드
```yaml
dashboard: exec_daily
cache_ttl: 24h
invalidate_on: [warehouse.load_completed.sales_daily]
show_data_as_of: true
```
무엇이 깨졌나: 캐시 만료가 데이터 변경이 아니라 시간에만 묶여 있었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 적재 이벤트 무효화 | 이벤트 발행 | 작음 | 이벤트 누락 | 기본 |
| 기준 시각 표시 | 화면 수정 | 작음 | 사용자가 안 봄 | 기본 |
| 캐시 1시간 | 웨어하우스 용량 | 작음 | 부하 증가 | 대안 |

**결론**: 캐시는 데이터가 바뀌면 같이 버린다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
