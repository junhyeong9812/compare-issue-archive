# issue/sharefs/inode-exhaustion-temp-files — 정답

태그: `capacity` · `resource-limit`

## 정답
<!-- 질문 1:1 대응 -->

1. **있다.** inode가 바닥나면 새 파일을 못 만든다.

2. **경보는 용량(`df -h`)만 봤다.**

3. **대부분 회복된다.** 썸네일 프로세스가 연 파일 일부는 재기동 후 회수됐다.

4. **inode 사용률 80% 경보, 임시 디렉터리 하루 지난 파일 정리.**

## 문제 구조 (추상화 코드)

### 변형 A — 임시 파일 미삭제
① 문제 코드
```java
Path tmp = Files.createTempFile(shareTmp, "thumb", ".png");
render(src, tmp);
upload(tmp);
```
② 고친 코드
```java
Path tmp = Files.createTempFile(localTmp, "thumb", ".png");   // 공유 스토리지 대신 로컬
try { render(src, tmp); upload(tmp); }
finally { Files.deleteIfExists(tmp); }
```
무엇이 깨졌나: 용량만 자원으로 봤다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| finally 삭제 + 로컬 임시 | 코드 수정 | 작음 | 없음 | 기본 |
| inode 경보 | 메트릭 | 작음 | 없음 | 함께 |
| inode 많은 포맷으로 재생성 | 스토리지 작업 | 큼 | 작업 중단 | 선택하지 않은 방법 |

**결론**: 임시 파일은 로컬에서 만들고 지우며, inode를 용량과 함께 감시한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
