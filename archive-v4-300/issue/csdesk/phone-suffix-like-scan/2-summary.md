# issue/csdesk/phone-suffix-like-scan — 정리 (힌트)

태그: `performance` · `database`

## 전체 흐름

```
LIKE '%0957'  → 앞이 와일드카드 → B-tree 사용 불가 → 전체 스캔 1,900만 행
LIKE '010%'   → 접두 일치 → 인덱스 범위 스캔 가능

교정: phone_rev = reverse(phone) 생성 컬럼 + 인덱스
      WHERE phone_rev LIKE reverse('0957') || '%'
또는  phone_last4 생성 컬럼 + 인덱스 (정확히 4자리 검색만)
```

## 핵심 문장

- 앞에 `%`가 붙은 LIKE는 B-tree 인덱스를 못 쓴다 — **전체 스캔**이다.
- 시험 환경 데이터가 2만 행이라 스캔도 빨랐다.
- 뒷자리 4자리만 찾는다면 **생성 컬럼(last4) + 인덱스**가 가장 단순하다.
- 검색 기능 추가는 운영 규모 실행 계획 확인을 출시 조건으로 둔다.
