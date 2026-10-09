# issue/amlscan/ack-pickup-void — 정리 (힌트)

태그: `silent-failure` · `environment-drift`

## 전체 흐름

```
amlscan submit ──▶ FIU 보고망   SUBMITTED
                     │
           다음 날   결과 파일 ACC (형식 통과)
                     │
           접수증 ACK_<번호>.p7s 생성 ── 보고기관이 3영업일 안에 pull 해야 확정
                     │      pull 됨  → 제출 확정
                     │      안 됨    → 제출 무효 (FIU 집계 '미제출')

amlscan 4.x : submit 프로세스가 ACK 자동 pull
amlscan 5.0 : ACK pull 이 별도 명령 `amlscan ack-pull` 로 분리 → cron 등록 필요
              업그레이드 런북에 없음 → 미등록

9월 3건 = 수동으로 status --refresh 를 돌린 날 사이에 낀 건
```

## 핵심 문장

- FIU 보고는 결과 파일 `ACC`(형식 통과) 뒤 **접수증을 기관이 가져가야** 확정된다 — 3영업일 안에 안 가져가면 무효다.
- amlscan 5.0은 접수증 수거를 `ack-pull`로 분리했다 — **업그레이드 후 cron 등록**이 필요했다.
- 담당자가 가끔 실행한 `status --refresh`가 접수증도 함께 가져가서, 그 사이 제출만 살아남았다.
- 무효 제출은 되살릴 수 없다 — 재제출하고 지연 사유서를 낸다.
- 확인 기준은 `ACC`가 아니라 **접수증 보관 여부**다.
