# issue/kdeploy/jvm-heap-vs-container-limit-oomkill — 정리 (힌트)

태그: `resource-limit` · `config`

## 전체 흐름

```
limit 2Gi
 ├ heap 최대 1.7Gi (85%)  현재 1.2Gi
 └ 비힙: direct buffer(파일 압축) 450Mi + metaspace 150Mi + 스레드 100Mi
합계 1.9Gi → 순간 압축 버퍼 증가 → 2Gi 초과 → 커널 OOM kill (exit 137)
```

## 핵심 문장

- `MaxRAMPercentage`는 **힙만** 정한다 — 나머지 비힙 메모리를 위한 여유가 필요하다.
- 커널 OOM kill은 JVM의 OutOfMemoryError가 아니다 — 앱 로그 없이 exit 137로 끝난다.
- 힙 대시보드만 보면 안 보인다 — 컨테이너 RSS(`working_set`)를 봐야 한다.
- 힙 비율을 70% 안팎으로 낮추고 다이렉트 버퍼 상한을 둔다.
