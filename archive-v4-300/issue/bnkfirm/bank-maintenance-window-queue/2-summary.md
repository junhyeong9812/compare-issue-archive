# issue/bnkfirm/bank-maintenance-window-queue — 정리 (힌트)

태그: `async-status` · `external-dependency`

## 전체 흐름

```
즉시지급 23:55 → bnkfirm transfer → 응답 ACCEPTED (st QUEUED_MAINT)
수취 은행 점검 23:50~00:20
      │  중계는 점검 은행 이체를 대기열에 보관
      ▼
00:20 이후 일괄 전송 → 입금
사내: ACCEPTED 를 입금 완료로 표시 → 가맹점 문의
```

## 핵심 문장

- `bnkfirm`의 ACCEPTED에는 **즉시 전송**과 **점검 대기(QUEUED_MAINT)**가 같이 들어 있다 — 상태 필드로만 구분된다.
- 은행마다 점검 시간이 다르고, 수취 은행이 점검인 건만 늦었다.
- 교정: QUEUED_MAINT면 가맹점 화면에 '은행 점검으로 지연'을 표시하고, 완료 판정은 DONE 상태로.
