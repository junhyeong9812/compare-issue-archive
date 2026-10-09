# issue/prodsrch/user-dictionary-update-no-reindex — 정답

태그: `deployment` · `search-quality` · `consistency`

## 정답
<!-- 질문 1:1 대응 -->

1. **질의는 '무선이어폰' 한 토큰, 기존 문서는 '무선'·'이어폰' 두 토큰이라 맞는 문서가 없었다.**

2. **사전 파일 배포 후 재색인 단계가 없었다.**\
분석기 재시작만 했다.

3. **신규 상품은 새 사전으로 색인돼 토큰이 맞았다.**

4. **사전 갱신 = 새 색인 생성 → 상위 검색어 1,000개 결과 수 비교 → 전환을 한 절차로.**

## 문제 구조 (추상화 코드)

### 변형 A — 사전만 바꾸고 색인은 그대로
① 문제 코드
```sh
cp user_dict.txt /etc/prodsrch/dict/ && prodsrch_restart
```
② 고친 코드
```sh
cp user_dict.txt /etc/prodsrch/dict/
build_new_index products_$(date +%Y%m%d)       # 새 사전으로 전체 색인
compare_top_queries products_current products_$(date +%Y%m%d) || exit 1
switch_index products_$(date +%Y%m%d)
```
무엇이 깨졌나: 색인 시점과 검색 시점의 분석 규칙이 갈렸다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 재색인 후 전환 | 색인 용량 2배 | 중간 | 전환 중 증분 누락 | 기본 |
| 상위 검색어 회귀 점검 | 검색어 목록 | 작음 | 롱테일 미검출 | 기본 |
| 검색 분석기만 동의어 처리 | — | 작음 | 규칙 복잡 | 임시 |

**결론**: 사전은 색인과 함께 바꾼다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
