# issue/alertrt/route-first-match-team-not-paged — 정답

태그: `config` · `observability`

## 정답
<!-- 질문 1:1 대응 -->

1. **인프라팀으로만 간다.**

2. **테스트 경보에 `namespace` 라벨이 없었다.**

3. **인프라팀에 보내고 이어서 정산팀에도 보낸다.**

4. **라우팅 설정 변경 시 실제 경보 라벨 샘플로 `routes test`를 CI에서 돌린다.**

## 문제 구조 (추상화 코드)

### 변형 A — 팀 라우트가 일반 라우트 아래
① 문제 코드
```yaml
routes:
  - { matchers: [namespace="batch"], receiver: infra }
  - { matchers: [team="settle"], receiver: settle }
```
② 고친 코드
```yaml
routes:
  - { matchers: [team="settle"], receiver: settle, continue: true }
  - { matchers: [namespace="batch"], receiver: infra }
```
무엇이 깨졌나: 라우팅 순서의 의미를 테스트 경보가 검증하지 못했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 팀 라우트 상단 + continue | 설정 | 작음 | 중복 알림 | 기본 |
| routes test CI | 라벨 샘플 | 작음 | 샘플 누락 | 함께 |
| 모든 라우트 continue | 없음 | 작음 | 알림 폭주 | 선택하지 않은 방법 |

**결론**: 팀 소유 경보를 먼저 라우팅하고 CI에서 실제 라벨로 검증한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
