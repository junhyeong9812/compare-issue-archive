# issue/settlebat/container-timezone-utc-date-shift — 정리 (힌트)

태그: `deploy` · `timezone`

## 전체 흐름

```
베이스 이미지 교체 (보안 패치) → TZ 미설정 → JVM 기본 UTC
00:30 KST 배치 시작 = 15:30 UTC 전날
LocalDate.now() → 전날 → 정산 대상일 하루 밀림
```

## 핵심 문장

- `LocalDate.now()`는 **JVM 기본 시간대**를 쓴다 — 이미지 설정에 따라 달라진다.
- 옛 이미지는 TZ=Asia/Seoul이 들어 있었다.
- 교정: 업무 날짜 계산은 명시 시간대(Clock 주입), 기동 시 시간대 확인 로그.
