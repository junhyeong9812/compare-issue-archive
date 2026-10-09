# issue/kdeploy/configmap-subpath-not-updated — 정리 (힌트)

태그: `environment-drift` · `stale-config`

## 전체 흐름

```
ConfigMap fee 수정
 ├ 디렉터리 마운트: kubelet 이 심볼릭 링크 교체 (1분 내 반영)
 └ subPath 마운트: 기동 시 파일을 바인드 → 이후 갱신 없음
결과: 수정 후 새로 뜬 파드(오토스케일) = 새 값 / 기존 파드 = 옛 값
```

## 핵심 문장

- **subPath 마운트는 ConfigMap 갱신을 받지 않는다** — 파드가 다시 떠야 바뀐다.
- 오토스케일로 새로 뜬 파드만 새 값을 읽어 **같은 서비스 안에서 값이 갈렸다**.
- 디렉터리 마운트는 kubelet 동기화 주기(최대 약 1분) 뒤 바뀐다 — 즉시는 아니다.
- 설정 변경은 ConfigMap 이름에 해시를 붙여 **롤링 재기동**으로 반영하는 것이 확실하다.
