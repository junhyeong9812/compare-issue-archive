# issue/iamctl/break-glass-untagged-grant — 정리 (힌트)

태그: `security` · `silent-failure`

## 전체 흐름

```
A: iamctl grant --break-glass --ttl 4h  → 부여 + 태그 expires=06-02T18:10
B: 콘솔 '권한 추가'                      → 부여 (태그 없음)

만료 배치 (10분마다): 태그 expires < now 인 부여만 회수
  A → 18:10 회수   B → 대상 아님 → 영구 권한
dry-run 도 같은 조건 → B 안 보임
```

## 핵심 문장

- 만료 배치는 **`expires` 태그가 있는 부여만** 본다 — 콘솔 수동 부여에는 태그가 없다.
- dry-run도 같은 조건이라 **남은 권한을 보여 주지 않는다**.
- 긴급 권한은 **CLI 한 경로**로만 주고, 콘솔의 고위험 역할 부여는 막는다.
- 고위험 역할 보유자 일일 목록과 근거 티켓 대사를 둔다.
