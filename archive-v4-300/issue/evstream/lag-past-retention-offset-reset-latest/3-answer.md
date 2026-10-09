# issue/evstream/lag-past-retention-offset-reset-latest — 정답

태그: `silent-failure` · `data-loss`

## 정답
<!-- 질문 1:1 대응 -->

1. **토픽의 최신 끝부터다.** 커밋 오프셋이 이미 삭제된 구간을 가리켜 reset 정책이 적용됐고, 정책이 `latest`였다.

2. **`latest`는 남은 로그를 보지 않고 끝으로 간다.** `earliest`였다면 삭제된 8시간분만 잃었을 것이다.

3. **있다.** 재처리 구간과 이미 처리된 구간이 겹칠 수 있다 — 결제 id 유니크 키로 적립을 멱등하게 만들어 함께 적용했다.

4. **소비자 그룹의 오프셋 리셋 로그와 토픽 로그 시작 오프셋을 본다.** 리셋 기록이 있으면 이 사건이다.

## 문제 구조 (추상화 코드)

### 변형 A — latest 리셋 + 비활성 그룹 감시 없음
① 문제 코드
```properties
group.id=point-earner
auto.offset.reset=latest
enable.auto.commit=true
```
② 고친 코드
```properties
group.id=point-earner
auto.offset.reset=earliest        # 적립은 결제 id 유니크로 멱등
enable.auto.commit=false          # 처리 후 커밋
# 경보: 그룹 비활성 30분 / 커밋 오프셋과 로그 시작 거리 < 1일분
```
무엇이 깨졌나: "처음 보는 소비자"용 기본 정책이 "오래 멈춘 소비자"에게도 적용된다는 점을 몰랐다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| earliest + 멱등 소비 | 멱등 키 | 작음 | 대량 재처리 시간 | 기본 |
| 보존 기간 연장(14일) | 디스크 | 중간 | 더 긴 장애엔 같음 | 함께 적용 |
| 비활성 그룹 경보 | 모니터링 | 작음 | 없음 | 항상 |

**결론**: reset 정책은 earliest로 두고 소비를 멱등하게 만들며, 멈춘 소비자 그룹을 경보로 잡는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
