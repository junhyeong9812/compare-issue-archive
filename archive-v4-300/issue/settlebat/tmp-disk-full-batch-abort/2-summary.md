# issue/settlebat/tmp-disk-full-batch-abort — 정리 (힌트)

태그: `infra` · `resource-exhaustion`

## 전체 흐름

```
settlebat: 카드사별 정렬 중간 파일 /var/settle/tmp/*.sort (일 40GB)
정상 종료 시에만 삭제 → 실패·중단된 회차 파일 잔존
      │  6개월 누적
      ▼
디스크 500GB 가득 → 쓰기 실패 → 배치 중단
```

## 핵심 문장

- 정리 코드가 **정상 종료 경로에만** 있었다.
- 디스크 사용률 경보가 80%에 있었지만 수신자가 퇴사자였다.
- 교정: 회차별 임시 디렉터리 + 시작 시 오래된 회차 정리, 경보 수신자 팀 채널로.
