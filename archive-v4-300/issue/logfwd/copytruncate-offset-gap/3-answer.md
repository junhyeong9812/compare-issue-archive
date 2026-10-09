# issue/logfwd/copytruncate-offset-gap — 정답

태그: `silent-failure` · `environment-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **파일이 다시 800MB를 넘은 뒤부터, 그것도 800MB 이후 부분만 수집된다.**\
`copytruncate`는 파일을 복사한 뒤 원본을 0바이트로 자른다. inode는 그대로다.\
`logfwd`는 inode로 파일을 식별하고, 크기가 오프셋보다 작으면 "아직 새 줄이 없다"고 보고 기다린다.
   > **copytruncate** — 로그 파일을 이름 바꾸기 대신 복사 후 원본 자르기로 회전하는 logrotate 방식. 앱이 파일을 다시 열 필요가 없지만 inode가 유지된다.

2. **00:00 이후 쓰인 로그가 오프셋 800MB에 닿을 때까지 전부 무시됐기 때문이다.**\
에이전트는 오류를 내지 않았고 상태도 "running"이었다. 검색 화면에서만 구간이 비었다.

3. **전날 로그 크기를 다시 쓰는 데 걸리는 시간에 비례한다.**\
전날(행사일)이 바쁘고 다음 날 새벽이 한가하면 공백이 몇 시간까지 길어진다. 이번 장애일은 행사 다음 날이라 공백이 03:40까지 이어졌다.

4. **원래 손실은 복사와 자르기 사이(밀리초~초)의 몇 줄이다.** 이 사건은 그와 달리 에이전트의 판단 때문에 회전 이후 수십 MB~수백 MB가 빠진다.\
즉 `copytruncate` 자체의 경쟁 손실보다 몇 자릿수 크다.

5. **앱이 회전 후 새 파일을 다시 열어야 한다.**\
`create` 방식은 이름을 바꾸고 새 파일을 만들기 때문에, 앱이 예전 파일 핸들을 쥐고 있으면 회전된 파일에 계속 쓴다.\
로깅 프레임워크의 자체 회전(시간·크기 기반 롤링)으로 옮기거나, logrotate `postrotate`에서 앱에 재오픈 신호를 보낸다.

6. **시간대별 "앱이 쓴 줄 수"와 "수집된 줄 수"를 비교한다.** 앱 쪽에는 로그 줄 카운터 메트릭을 두고, 수집 쪽에는 인덱스 문서 수를 둔다.\
회전 시각 직후 비율이 급락하면 경보한다.

## 문제 구조 (추상화 코드)

### 변형 A — inode 기준 추적 + 잘림 미감지
① 문제 코드
```python
st = os.stat(path)
if st.st_ino == state.ino:
    if st.st_size > state.offset:
        state.offset = read_from(path, state.offset)   # 잘린 경우(size < offset)는 그냥 대기
else:
    state = new_state(st.st_ino, 0)
```
② 고친 코드
```python
st = os.stat(path)
if st.st_ino != state.ino or st.st_size < state.offset:   # 잘림 = 새 파일로 취급
    state = new_state(st.st_ino, 0)
state.offset = read_from(path, state.offset)
```
무엇이 깨졌나: "같은 inode = 이어서 읽기"라는 가정이 copytruncate 회전에서 깨졌다.

### 변형 B — 회전 설정
① 문제 코드
```
/var/log/app/app.log { daily; copytruncate; rotate 7 }
```
② 고친 코드
```
/var/log/app/app.log { daily; create; rotate 7; postrotate kill -USR1 $(cat /run/app.pid); endscript }
```
무엇이 깨졌나: 회전 방식과 수집기 추적 방식의 조합을 확인하지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| `create` + 앱 재오픈 | 앱이 재오픈 신호 지원 | 중간 | 신호 누락 시 옛 파일에 계속 기록 | 기본 |
| 에이전트 `truncate_detect on` | 에이전트 설정 권한 | 작음 | 복사-자르기 사이 몇 줄 손실은 남음 | 앱 수정이 어려울 때 |
| 프레임워크 자체 롤링 | 로깅 설정 변경 | 중간 | 다중 프로세스 동시 쓰기 시 충돌 | 단일 프로세스 앱 |

**결론**: 이번에는 에이전트 옵션을 먼저 켜 공백을 막고, 앱별로 `create` 전환을 진행했다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
