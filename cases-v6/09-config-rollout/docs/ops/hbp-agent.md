# hbp-agent (서버 설정 에이전트) 메모

플랫폼팀 · 2025-03

- 각 결제 서버에서 돈다. 배포 디렉터리의 `hosts/<서버>/app.conf`만 본다(같은 폴더의 다른 파일은 보지 않는다).
- 60초마다 파일의 sha256을 이전 값과 비교한다. 바뀌었으면 가져와서 앱을 graceful restart 한다.
  - 앱은 설정 다시 읽기(reload)를 지원하지 않는다.
  - graceful restart: 새 프로세스가 새 DB 커넥션 풀을 다 연 뒤 이전 프로세스가 요청을 마저 처리하고 풀을 닫는다(겹치는 구간 약 40초).
- sha256이 같으면 아무것도 하지 않는다(mtime만 바뀐 경우 포함).
- 에이전트 로그: `/var/log/hbp-agent/agent.log` — "config changed, restarting app" 줄.
