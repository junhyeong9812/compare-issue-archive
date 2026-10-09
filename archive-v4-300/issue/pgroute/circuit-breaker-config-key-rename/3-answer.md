# issue/pgroute/circuit-breaker-config-key-rename — 정답

태그: `config-drift` · `deploy`

## 정답
<!-- 질문 1:1 대응 -->

1. **키 이름이 2.x에서 바뀌어 옛 키는 무시되고 기본값이 쓰였다.** 기동 로그에도 경고가 없었다.

2. **적용된 실제 설정값을 운영 엔드포인트로 읽어 기대값과 비교한다.** 파일 내용이 아니라 적용값을 본다.

3. **공용 설정 파일에 다른 컴포넌트용 키가 섞여 있으면 기동이 막힌다.** 접두(namespace) 단위로만 엄격 검사를 켰다.

4. **운영 콘솔에서 PG-C 가중치를 0으로 수동 변경했다.** 40분 중 32분이 사람이 알아채기까지 걸린 시간이었다.

## 문제 구조 (추상화 코드)

① 문제 코드
```yaml
resilience:
  pgC: { failureRateThreshold: 30, ringBufferSizeInClosedState: 50 }
```
② 고친 코드
```yaml
resilience:
  pgC: { failureRateThreshold: 30, slidingWindowSize: 50, minimumNumberOfCalls: 10 }
# + 기동 시 resilience.* 아래 미지 키 → 실패
```
무엇이 깨졌나: 설정 이름 변경이 조용한 기본값 적용으로 이어졌고, 적용값 확인이 없었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 미지 키 엄격 검사 + 적용값 노출 | 작음 | 공용 파일 충돌 | 기본 |
| 업그레이드 체크리스트만 | 작음 | 사람이 놓침 | 보조 |

**결론**: 설정은 적용값으로 확인하고, 모르는 키는 기동 단계에서 막는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
