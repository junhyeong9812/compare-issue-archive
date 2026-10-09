# issue/bidash/row-permission-group-reorg-blank — 정답

태그: `permission` · `operations` · `ux`

## 정답
<!-- 질문 1:1 대응 -->

1. **신설 조직 그룹이 권한 매핑표에 없어 볼 수 있는 행이 0이었다.**

2. **행 수준 권한은 거부가 아니라 필터라서 결과가 빈 표였다.**

3. **기존 팀 그룹은 매핑이 있었다.**

4. **매핑 없는 그룹은 명시적 안내, 조직 변경 시 매핑 누락 경보, 개편 체크리스트에 BI 권한.**

## 문제 구조 (추상화 코드)

### 변형 A — 매핑 없음을 빈 허용 범위로 처리
① 문제 코드
```python
allowed = PERM_MAP.get(user.group, set())
return query.filter(category__in=allowed)
```
② 고친 코드
```python
if user.group not in PERM_MAP:
    raise NoDashboardPermission(user.group)     # 화면에 안내 + 관리자 알림
return query.filter(category__in=PERM_MAP[user.group])
```
무엇이 깨졌나: "설정 없음"과 "볼 것 없음"을 같은 결과로 표현했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 매핑 없음 명시 안내 | 화면 수정 | 작음 | — | 기본 |
| 조직 변경 이벤트 경보 | 인사 연동 | 중간 | 이벤트 지연 | 기본 |
| 기본 전체 허용 | — | 작음 | 정보 노출 | 선택하지 않은 방법 |

**결론**: 권한 설정 누락은 빈 결과가 아니라 안내로 드러낸다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
