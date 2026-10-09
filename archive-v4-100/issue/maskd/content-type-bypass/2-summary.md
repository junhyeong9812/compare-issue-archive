# issue/maskd/content-type-bypass — 정리 (힌트)

태그: `security` · `privacy`

## 전체 흐름

```
백오피스 ──응답──▶ maskd ──▶ 상담 화면
  Content-Type: application/json         → 마스킹 적용
  Content-Type: application/problem+json → 대상 아님 → 원문 통과   ← 2026-03 오류 형식 변경
  Content-Type: text/plain               → 대상 아님 → 원문 통과

오류 detail: "고객 900101-1****** 조회 실패..." 가 아니라 원문 전체(검증 예외 메시지에 요청값 포함)
```

## 핵심 문장

- maskd의 대상은 **Content-Type이 정확히 `application/json`**인 응답이다 — `+json` 접미 형식은 빠진다.
- 오류 형식 표준화(RFC 9457) 작업이 마스킹 범위를 조용히 벗어났다.
- 검증 예외 메시지가 **요청값을 그대로 담아** 오류 본문에 개인정보가 들어갔다.
- 프록시는 처리 못 하는 형식을 **통과가 아니라 차단(또는 전체 마스킹)**해야 한다.
