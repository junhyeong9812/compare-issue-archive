# issue/settlebat/tmp-disk-full-batch-abort — 정답

태그: `infra` · `resource-exhaustion`

## 정답
<!-- 질문 1:1 대응 -->

1. **경보 수신자가 퇴사자 개인 메일이었다.**

2. **배치 시작 시 3일 지난 회차 디렉터리를 지우고, 종료 시 finally로 현재 회차를 정리.**

3. **실패 회차는 3일 보관 후 정리한다.**

## 문제 구조 (추상화 코드)

① 문제 코드
```java
run(); Files.deleteIfExists(tmp);           // 정상 종료에만
```
② 고친 코드
```java
cleanupOlderThan(tmpRoot, Duration.ofDays(3));
Path dir = Files.createDirectories(tmpRoot.resolve(runId));
try { run(dir); } finally { if (succeeded) deleteRecursively(dir); }
```
무엇이 깨졌나: 자원 정리가 정상 경로에만 있었고 경보가 죽어 있었다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 회차 디렉터리 + 시작 시 정리 | 작음 | 없음 | 기본 |
| 디스크 증설 | 비용 | 다시 참 | 선택하지 않음 |

**결론**: 임시 자원은 실패 경로에서도 정리되게 하고, 경보 수신자를 사람 아닌 팀으로 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
