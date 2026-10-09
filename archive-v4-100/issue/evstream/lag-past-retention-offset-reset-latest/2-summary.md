# issue/evstream/lag-past-retention-offset-reset-latest — 정리 (힌트)

태그: `silent-failure` · `data-loss`

## 전체 흐름

```
topic pay.completed  retention 7d
point-earner 장애(배포 실패 방치) 4/1 02:00 ~ 4/8 10:00  (7일 8시간)
  커밋 오프셋 1,203,551 → 이 구간 세그먼트는 4/8 02:00 이후 삭제
재기동: 커밋 오프셋이 로그 시작보다 앞 → auto.offset.reset=latest → 최신부터 소비
  4/1 02:00 ~ 4/1 10:00 (삭제됨) + 4/1 10:00 ~ 4/8 10:00 (남아 있었는데도) 건너뜀
```

## 핵심 문장

- 커밋 오프셋이 **로그 시작보다 앞**이 되면 `auto.offset.reset` 정책이 적용된다 — `latest`면 남아 있는 이벤트까지 전부 건너뛴다.
- 실제로 지워진 것은 8시간분이었지만, `latest` 때문에 **7일치 전체**를 건너뛰었다.
- 소비자 지연(lag) 경보가 소비자 그룹이 **비활성**일 때는 계산되지 않아 7일간 울리지 않았다.
- 대처: `earliest`로 두고 멱등 적립(결제 id 유니크), 비활성 그룹 지연 경보, 삭제된 8시간분은 결제 원장에서 재생성.
