# issue/batchd/container-utc-file-date — 정리 (힌트)

태그: `environment-drift` · `timezone`

## 전체 흐름

```
VM 시절: TZ=Asia/Seoul / 컨테이너: TZ 미설정 → UTC
03:00 KST 실행 = 18:00 UTC 전날
파일명 = LocalDate.now() → 전날 날짜 → 파트너 수신 측이 "어제 파일 재전송"으로 판단해 무시
```

## 핵심 문장

- 컨테이너 기본 시간대는 **UTC**다 — VM에서 암묵적으로 쓰던 서울 시간이 사라졌다.
- 코드가 `LocalDate.now()`처럼 **시스템 시간대에 기대는 API**를 썼다.
- 00:00~09:00 KST 사이에 도는 배치 11개가 영향을 받았다.
- 대처: 업무 날짜는 실행 파라미터로 명시(`--biz-date`), 코드는 `ZoneId.of("Asia/Seoul")`을 명시.
