# issue/ledgerctl/reimage-config-owner-root — 정리 (힌트)

태그: `environment-drift` · `deployment` · `permission`

## 전체 흐름

```
재이미징 → 설정 백업 복원 (sudo tar -x ...)
   /etc/ledgerctl/client.conf   소유자 root:root, 0600   ← 배치 계정 batch 가 못 읽음
   점검: 담당자가 sudo 로 ledgerctl ... 실행 → 성공 (root 는 읽힘)
03:00 크론(batch 계정) → 설정 읽기 실패 → 자격증명 없이 접속 → 인증 오류 → 전 건 실패
2호기: 복원 스크립트에 chown batch:batch 단계가 있는 최신 판 사용 → 정상
```

## 핵심 문장

- 원인은 자격증명 값이 아니라 **설정 파일 소유자** — 백업 복원을 root로 해서 batch 계정이 읽지 못했다.
- 수동 점검을 **sudo로** 해서 성공했다 — 점검이 실제 실행 계정과 달라 문제를 못 봤다.
- 2호기는 소유자 복원 단계가 들어간 최신 절차서를 썼다 — **절차서 두 판**이 공존했다.
- 대처는 소유자·권한 복원 단계 표준화, 점검은 **배치 계정으로**(sudo -u batch) 실행.
