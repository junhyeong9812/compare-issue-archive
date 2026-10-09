# issue/iamctl/break-glass-untagged-grant — 정답

태그: `security` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **A는 회수, B는 그대로다.** B의 부여에는 만료 태그가 없어 배치가 건드리지 않는다.

2. **콘솔의 '권한 추가'는 일반 부여 화면이라 만료 개념이 없다.** 장애 중 CLI를 못 쓰는 노트북에서 콘솔을 썼다.

3. **보여 주지 않는다.** dry-run은 '회수될 것'만 출력한다 — '회수되지 않을 고위험 권한'은 보이지 않는다.

4. **3주 뒤 분기 접근 검토에서 B의 운영 DB 쓰기 권한이 근거 티켓 없이 나왔다.** 사용 기록은 장애 당일뿐이었다.

5. **고위험 역할은 콘솔 부여를 막고 `--break-glass`만 허용**한다. 일일 보고서로 고위험 역할 보유자와 근거 티켓을 대사한다.

## 문제 구조 (추상화 코드)

### 변형 A — 태그 기반 회수
① 문제 코드
```sql
DELETE FROM grants
WHERE tags->>'expires' IS NOT NULL AND (tags->>'expires')::timestamptz < now();
```
② 고친 코드
```sql
-- 고위험 역할은 태그가 없으면 즉시 경보
SELECT * FROM grants g JOIN roles r USING (role_id)
WHERE r.risk = 'high' AND g.tags->>'expires' IS NULL;
DELETE FROM grants WHERE (tags->>'expires')::timestamptz < now();
```
무엇이 깨졌나: 회수 대상을 '만료 정보가 있는 것'으로 정의해, 만료 정보 없이 생긴 권한은 영구가 됐다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 고위험 역할 콘솔 부여 차단 | 콘솔 수정 | 작음 | 장애 중 불편 | 기본 |
| 태그 없는 고위험 부여 경보 | 일일 조회 | 작음 | 없음 | 함께 |
| 분기 접근 검토 | 사람 | 큼 | 느림 | 기존 안전망 |

**결론**: 긴급 권한은 만료가 강제되는 한 경로로만 준다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
