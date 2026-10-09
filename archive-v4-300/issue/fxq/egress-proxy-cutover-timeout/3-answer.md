# issue/fxq/egress-proxy-cutover-timeout — 정답

태그: `environment-drift` · `latency` · `observability`

## 정답
<!-- 질문 1:1 대응 -->

1. **호출마다 첫 연결의 30초 타임아웃이었다.**\
프록시 이름이 IPv6 주소도 돌려줬고, 첫 시도가 그쪽으로 가서 응답 없이 30초를 기다린 뒤 IPv4로 재시도해 성공했다.

2. **옛 프록시는 IPv6도 받았고, 새 프록시는 IPv4만 받았기 때문이다.**\
교체 작업은 A 레코드만 새 장비로 돌렸고 AAAA 레코드는 옛 주소 그대로 남겼다.

3. **사내 API 도메인에는 AAAA 레코드가 없어 처음부터 IPv4로 갔다.**\
같은 서버라도 이름별로 결과가 달랐다.

4. **회계팀이 "평가 리포트가 9시 회의 전에 안 온다"고 문의해서다.**\
배치 단계별 소요 시간을 보니 환율 조회만 20배로 늘어 있었다.

5. **재시도가 성공하면 실패 신호가 사라진다.**\
재시도는 결과를 지키지만 원인을 지운다 — 재시도 횟수와 지연을 별도 지표로 남겨야 한다.

## 문제 구조 (추상화 코드)

### 변형 A — 재시도 성공을 무신호 정상으로 처리
① 문제 코드
```python
for attempt in range(2):
    try:
        return run_fxq(args, timeout=30)
    except Timeout:
        continue                     # 기록 없음
```
② 고친 코드
```python
for attempt in range(2):
    try:
        out = run_fxq(args, timeout=30)
        metrics.incr("fxq.retry", attempt)      # 재시도 횟수 지표
        return out
    except Timeout:
        log.warning("fxq timeout attempt=%d", attempt)
```
무엇이 깨졌나: 재시도 성공을 정상과 구별하지 않아 네트워크 설정 오류가 닷새 동안 지연으로만 나타났다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| AAAA 레코드 정리 | 프록시가 IPv6 미지원 | DNS 변경 1건 | 향후 IPv6 도입 시 재등록 누락 | 기본 |
| 클라이언트 IPv4 강제 | 실행 환경 변수 제어 가능 | 작음 | 다른 이름에도 일괄 적용돼 숨김 | 임시 |
| 단계별 소요 시간 경보 | 기준선 수집 | 대시보드 1개 | 경보 피로 | 기본(병행) |

**결론**: 프록시 교체 체크리스트에 A·AAAA 레코드 둘 다를 넣고, 재시도는 지표로 남긴다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
