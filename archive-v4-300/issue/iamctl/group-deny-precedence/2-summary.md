# issue/iamctl/group-deny-precedence — 정리 (힌트)

태그: `security` · `policy`

## 전체 흐름

```
cs0733 ∈ cs-team2            → customer:read  allow
       ∈ outsourced-legacy   → customer:read  deny (명시)
평가: 명시 deny 하나라도 있으면 deny  → 403

같은 팀 다른 상담원: 정규직 전환 시 legacy 그룹에서 빠짐 → allow
legacy 그룹: 2023 외주 계약 종료 때 '조회 금지'로 deny 추가, 그룹 자체는 삭제 안 함
```

## 핵심 문장

- iamctl 정책 평가는 **명시 deny 우선**이다 — 허용 그룹이 몇 개든 deny 하나가 이긴다.
- 개편 때 신규 그룹만 추가하고 **구 그룹 소속을 정리하지 않은** 사람만 403이 났다.
- `iamctl explain`은 어떤 그룹의 어떤 문장이 결정했는지 보여 준다.
- 개편 절차는 '추가'와 '제거'를 한 변경으로 묶고, deny 그룹 소속자 목록을 확인한다.
