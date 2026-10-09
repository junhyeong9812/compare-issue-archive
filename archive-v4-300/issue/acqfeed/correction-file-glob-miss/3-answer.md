# issue/acqfeed/correction-file-glob-miss — 정답

태그: `silent-failure` · `contract-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **정상 매입만 반영된다.** `.R01`은 패턴에 맞지 않아 목록 단계에서 빠지고, 출력·로그 어디에도 나오지 않는다. 정정 대상 212건의 금액이 정정 전 값으로 남았다.

2. **카드사가 전문 개정으로 정정 전달 방식을 바꿨기 때문이다.** 개정 공지는 받았지만 "레이아웃 변경 없음"이라 수집 설정 검토 대상에서 빠졌다. 파일 레이아웃은 같고 파일 이름 규칙만 바뀌었다.

3. **아니다.** 카드사 디렉터리에는 `.CHK`(검증용 해시)·`.TMP`(전송 중) 파일도 있다. `.TMP`를 읽으면 잘린 파일을 적재한다. `'*.DAT,*.R0?'`처럼 명시 목록으로 넓히고, 정정 파일은 `.CHK` 짝이 있을 때만 읽는다.

4. **원격 목록(`acqfeed ls --remote`)과 수집 이력을 나란히 놓는다.** 원격에 있고 이력에 없으면 수집 쪽, 원격에도 없으면 카드사 쪽이다. 수집 건수만 보는 모니터링은 "0건 수집"만 잡고 "일부 파일 누락"은 못 잡는다.

## 문제 구조 (추상화 코드)

① 문제 코드
```python
files = sftp.list(remote_dir, pattern=cfg.pattern)   # '*.DAT'
for f in files: load(f)
```
② 고친 코드
```python
remote = sftp.list(remote_dir)                        # 전체 목록
wanted = [f for f in remote if f.suffix in ('.DAT',) or CORR.match(f.name)]
missing = set(remote_business_files(remote)) - set(wanted) - history.done()
if missing: alert('unclassified remote files', missing)
for f in wanted: load_or_correct(f)                   # 정정분은 원매입 키로 덮어쓰기
```
무엇이 깨졌나: 수집 범위를 이름 패턴 하나에 맡겨, 상대의 이름 규칙 변경이 조용한 누락이 됐다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 패턴 확장 + 원격 목록 차집합 경보 | 작음 | 새 접미가 또 생기면 경보로만 드러남 | 기본 |
| 카드사에 정정분을 다음 날 `.DAT`로 합쳐 달라고 요청 | 협의 비용 | 카드사 거절, 하루 지연 | 선택하지 않음 |

**결론**: 패턴을 명시 목록으로 넓히고, 원격 목록과 수집 이력의 차집합을 매일 경보한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
