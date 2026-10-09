# issue/auditsink/field-mapping-conflict-drop — 정리 (힌트)

태그: `silent-failure` · `schema`

## 전체 흐름

```
일별 인덱스 audit-YYYYMMDD  (템플릿에 actor_id 매핑 없음 → 동적 매핑)
  그날 첫 문서 actor_id = 10234   → long 으로 고정
  이후 actor_id = "ADM-07"         → 매핑 오류 → 문서 거부 → 수집기 dead-letter
  그날 첫 문서 actor_id = "ADM-03" → keyword 로 고정 → 이후 숫자 사번도 문자열로 저장 OK

dead-letter 보관 7일 → 이후 삭제
```

## 핵심 문장

- 인덱스 템플릿에 `actor_id` 매핑이 없어 **그날 첫 문서의 값 형태가 타입을 정했다**.
- 숫자 사번이 먼저 오면 long이 되고, 이후 `ADM-07` 같은 문자열 ID 문서는 **거부**된다.
- 외주 운영자가 첫 작업을 한 날은 keyword가 돼서 모두 저장됐다 — '날에 따라'의 원인.
- 거부 문서는 dead-letter에 **7일만** 남는다 — 그 전에 재처리해야 한다.
- 식별자 필드는 템플릿에서 **keyword로 고정**한다.
