# issue/kdeploy/configmap-subpath-not-updated — subPath로 마운트한 ConfigMap 파일은 갱신되지 않는다 — 질문

## 질문
1. (예측) `fee.yaml`을 ConfigMap에서 subPath로 `/app/conf/fee.yaml`에 마운트했다. 앱은 파일 변경을 감지해 다시 읽는다. ConfigMap을 수정하면 실행 중 파드의 파일은?
2. (왜) 일부 파드는 새 수수료를 썼다. 어떤 파드였나?
3. (경계) subPath 없이 디렉터리 마운트였다면 즉시 바뀌는가?
4. (연결) 설정 변경 반영을 확실히 하려면?

## 복습 기록
| 날짜 | 결과 | 틀린 질문 | 다음 복습 |
|------|------|-----------|-----------|
| | | | |
