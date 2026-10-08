# rollout — 결제 서버 앱 설정 배포

릴리스 파일(`releases/<id>.rel`)의 설정 변경을 사내 설정 서비스에 기록하고, 서비스에 있는 값으로
서버별 `app.conf`를 만들어 배포 디렉터리에 둔다(각 서버의 에이전트가 가져간다).
설정 서비스는 벤더 CLI `vendor/bin/cfgsvc`(벤더가 바이너리로 제공)로 다룬다. 사용법은 `vendor/bin/cfgsvc --help`.

## 실행

```
bin/rollout apply r2026.10.08   # check → apply → render → verify → record
bin/rollout show api-02         # 서버에 배포된 app.conf
bin/rollout hosts               # 활성 서버와 풀
bin/rollout history             # 끝난 배포 이력(릴리스, 서비스 마크, 서버 수, 변경 수)
```

배포 파이프라인은 릴리스 승인 후 `bin/rollout apply <id>`를 돌리고, exit 0이 아니면 배포를 멈춘다.
`CFGSVC_HOME`(설정 서비스 상태 디렉터리)은 파이프라인 환경에서 지정한다.

## 구성

| 경로 | 내용 |
|------|------|
| `bin/rollout` | 진입점. 서브커맨드 분기, 단계 순차 실행 |
| `steps/NN-*.sh` | 단계 스크립트(번호 순). 각자 `_env.sh`로 lib·설정을 읽는다 |
| `lib/` | 공용 함수 (설정 서비스 래퍼, 조회 캐시, 범위 해석, 템플릿 렌더, 릴리스 파일, 이력 등) |
| `conf/rollout.conf` | 디렉터리·도구 경로. `ROLLOUT_*` 환경변수로 덮어쓴다 |
| `conf/hosts.conf` | 서버 목록(상태 D는 빠진 서버 — 설정을 만들지 않음) |
| `conf/app.conf.tpl` | 서버 설정 템플릿(`{{setting}}`) |
| `releases/` | 릴리스 파일 |
| `tests/` | `bash tests/run.sh` — 단위 테스트 + 릴리스 두 개 E2E |

## 설정 키

- 키는 `global.<setting>`, `pool.<풀>.<setting>`, `host.<서버>.<setting>` 세 범위다. 서버마다 가장 좁은 범위의 값이 이긴다(host → pool → global).
- 릴리스 파일은 `@release <id>` 한 줄과 `<키> = <값>` 줄들로 이뤄진다. 바뀌는 키만 적는다.
- 운영자가 장애 대응으로 서비스에 직접 넣은 값도 다음 배포 때 그대로 서버 설정에 반영된다(서비스가 기준이다).
