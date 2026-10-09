# issue/migrun/hotfix-lower-version-skipped — 정리 (힌트)

태그: `silent-failure` · `environment-drift`

## 전체 흐름

```
운영 이력: ... V20250510, V20250515 (최신)
배포:      V20250512__refund_reason.sql
migrun apply (out_of_order=false)
   └ "V20250512 < current V20250515 → ignored" (INFO 로그)  exit 0
스테이징: V20250515 적용 전이라 V20250512 정상 적용
```

## 핵심 문장

- `migrun`은 기본으로 **현재 최신 버전보다 낮은 번호**의 새 마이그레이션을 건너뛴다 — 오류가 아니라 INFO 로그다.
- 환경마다 적용 순서가 다르면 결과 스키마가 갈린다.
- 앱이 JSON 컬럼의 키로 저장하는 구조라 컬럼이 없어도 예외가 나지 않았다.
- 배포 후 `migrun validate --pending-is-error`로 미적용 마이그레이션을 실패로 만든다.
