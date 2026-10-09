# issue/bkvault/one-file-system-skips-new-volume — 정답

태그: `silent-failure` · `backup`

## 정답
<!-- 질문 1:1 대응 -->

1. **들어가지 않는다.** 빈 디렉터리만 기록된다.

2. **작업 상태는 성공이고 경고가 디버그 레벨이었다.** 같은 기간 DB 덤프가 커져 총량은 늘었다.

3. **9월 이전 백업 세트에는 마운트 전(디스크 A) 파일이 있었기 때문이다.** 9월 이후 생성분만 비었다.

4. **복구 후 원본과 경로별 파일 수를 비교한다.** 증설 체크리스트에 `bkvault plan --show-skipped` 확인을 넣었다.

## 문제 구조 (추상화 코드)

### 변형 A — 하위 마운트를 포함하지 않는 백업 설정
① 문제 코드
```yaml
job: settle-files
paths: [/data/settle]
one_file_system: true
```
② 고친 코드
```yaml
job: settle-files
paths: [/data/settle, /data/settle/attach]   # 마운트 지점을 명시
one_file_system: true
verify: { compare_file_count: true }
```
무엇이 깨졌나: 백업 범위가 경로가 아니라 파일 시스템 경계로 정해진다는 점을 몰랐다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 마운트 지점 명시 | 설정 관리 | 작음 | 다음 증설 누락 | 기본 |
| one_file_system 끄기 | 범위 통제 | 작음 | /proc 류 포함 위험 | 선택하지 않은 방법 |
| 복구 연습 파일 수 대조 | 연습 절차 | 중간 | 연습 주기만큼 지연 | 안전망 |

**결론**: 마운트 지점을 명시하고 백업 계획의 건너뛴 경로를 증설 절차에서 확인한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
